package edu.agile.sis.service;

import edu.agile.sis.dao.EnrollmentDAO;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * EnrollmentService - handles student-course registrations.
 *
 * Business rule implemented here:
 *  - GPA < 2.0  => max 5 courses
 *  - 2.0 <= GPA <= 3.0 => max 6 courses
 *  - GPA > 3.0 => max 7 courses
 *
 * GPA is expected as an attribute on the student entity stored in students collection.
 * The attribute key is searched case-insensitively for "gpa".
 */
public class EnrollmentService {
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final EntityService entityService = new EntityService("students");

    public EnrollmentService() {}

    public List<Document> listByStudent(String studentId) {
        return enrollmentDAO.listByStudent(studentId);
    }

    public List<Document> listByCourse(String courseCode) {
        return enrollmentDAO.listByCourse(courseCode);
    }

    public boolean isStudentRegisteredForCourse(String studentId, String courseCode) {
        return enrollmentDAO.find(studentId, courseCode) != null;
    }

    /**
     * Registers a student to a course enforcing GPA-based limits.
     *
     * Uses a distinct-course count from EnrollmentDAO to avoid off-by-one or duplicate issues.
     *
     * @throws IllegalStateException when business rule prevents registration (GPA limit exceeded, already registered, etc.)
     */
    public boolean registerStudentToCourse(String studentId, String courseCode) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Invalid student id");
        }
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("Invalid course code");
        }

        // Already registered? Use DAO find(...)
        if (enrollmentDAO.find(studentId, courseCode) != null) {
            throw new IllegalStateException("Student is already registered for course " + courseCode);
        }

        // Compute distinct course count for this student
        int distinctCount = enrollmentDAO.countDistinctCoursesByStudent(studentId);

        // get student's GPA from their attributes
        double gpa = readGpaFromStudent(studentId);

        int maxAllowed = computeMaxAllowedByGpa(gpa);

        // debug log (helps diagnose issues)
        System.out.println("[EnrollmentService] registerStudentToCourse -> studentId=" + studentId
                + ", courseCode=" + courseCode
                + ", gpa=" + gpa
                + ", maxAllowed=" + maxAllowed
                + ", distinctRegisteredCount=" + distinctCount);

        if (distinctCount >= maxAllowed) {
            throw new IllegalStateException(String.format(
                    "Registration denied: student GPA = %.2f allows max %d courses (currently registered: %d).",
                    gpa, maxAllowed, distinctCount));
        }

        // prepare enrollment doc
        Document enroll = new Document("studentId", studentId)
                .append("courseCode", courseCode)
                .append("registeredAt", new java.util.Date());

        // insert
        enrollmentDAO.insertEnrollment(enroll);
        return true;
    }

    public boolean unregisterStudentFromCourse(String studentId, String courseCode) {
        return enrollmentDAO.deleteByStudentAndCourse(studentId, courseCode);
    }

    // ---------- helpers ----------

    private int computeMaxAllowedByGpa(double gpa) {
        if (gpa < 2.0) return 5;
        if (gpa <= 3.0) return 6;
        return 7;
    }

    /**
     * Reads GPA from student entity attributes.
     * - looks into students.core and students.attributes for key "gpa" (case-insensitive)
     * - supports numeric values and string numeric representations
     * - if not found or invalid, returns 0.0
     */
    private double readGpaFromStudent(String studentId) {
        try {
            Document student = entityService.getEntityById(studentId);
            if (student == null) return 0.0;

            // 1) check core.gpa if present
            Document core = student.get("core", Document.class);
            if (core != null) {
                Object coreG = core.get("gpa");
                Double parsed = parseGpaObject(coreG);
                if (parsed != null) return parsed;
            }

            // 2) check attributes array for key "gpa" (case-insensitive)
            List<Document> attrs = student.getList("attributes", Document.class, new ArrayList<>());
            if (attrs != null) {
                for (Document a : attrs) {
                    if (a == null) continue;
                    String key = a.getString("key");
                    if (key != null && key.trim().equalsIgnoreCase("gpa")) {
                        Object val = a.get("value");
                        Double parsed = parseGpaObject(val);
                        if (parsed != null) return parsed;
                    }
                }
            }
        } catch (Exception ex) {
            // swallow and treat as 0.0
            ex.printStackTrace();
        }
        return 0.0;
    }

    private Double parseGpaObject(Object o) {
        if (o == null) return null;
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                String s = ((String) o).trim();
                if (s.isEmpty()) return null;
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
