package edu.agile.sis.service;

import edu.agile.sis.dao.AssignmentDAO;
import edu.agile.sis.util.FileStorageUtil;
import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

public class AssignmentService {
    private final AssignmentDAO dao = new AssignmentDAO();
    private final EnrollmentService enrollmentService = new EnrollmentService();

    public AssignmentService() {}

    public String createAssignment(String courseCode, String title, String description, Date dueDate,
                                   int points, String createdBy, List<Document> attachments) {
        Document doc = new Document("courseCode", courseCode)
                .append("title", title)
                .append("description", description)
                .append("dueDate", dueDate)
                .append("points", points)
                .append("createdBy", createdBy)
                .append("createdAt", new Date())
                .append("visible", true)
                .append("attachments", attachments == null ? List.of() : attachments);

        return dao.insertAssignment(doc);
    }

    /**
     * Returns assignments for a course ONLY if the student is registered in it.
     */
    public List<Document> listByCourseForStudent(String courseCode, String studentId) {
        if (enrollmentService.isStudentRegisteredForCourse(studentId, courseCode)) {
            return dao.listByCourse(courseCode);
        }
        return List.of(); // empty if not registered
    }

    /**
     * Returns assignment by ID only if student is enrolled in its course.
     */
    public Document getByIdForStudent(String id, String studentId) {
        Document assignment = dao.findById(id);
        if (assignment != null) {
            String courseCode = assignment.getString("courseCode");
            if (enrollmentService.isStudentRegisteredForCourse(studentId, courseCode)) {
                return assignment;
            }
        }
        return null; // not allowed
    }

    // teacher/admin (no restrictions)
    public List<Document> listByCourse(String courseCode) {
        return dao.listByCourse(courseCode);
    }

    public Document getById(String id) {
        return dao.findById(id);
    }

    public boolean update(String id, Document updates) { return dao.update(id, updates); }

    public boolean delete(String id) { return dao.delete(id); }

    public Document uploadAttachmentFromPath(String filePath, String filename, String contentType) throws IOException {
        try (InputStream is = new FileInputStream(filePath)) {
            String fileId = FileStorageUtil.uploadFile(FileStorageUtil.safeFilename(filename), is, contentType);
            return new Document("filename", filename).append("storageRef", fileId);
        }
    }
}
