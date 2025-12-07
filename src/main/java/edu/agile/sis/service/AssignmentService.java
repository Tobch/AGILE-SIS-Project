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


    public String createAssignment(String courseCode,
                                   String title,
                                   String description,
                                   Date dueDate,
                                   int points,
                                   String createdBy,
                                   List<Document> attachments) {

        Document doc = new Document()
                .append("courseCode", courseCode)
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

  
    public List<Document> listByCourse(String courseCode) {
        return dao.listByCourse(courseCode);
    }

   
    public List<Document> listByCourseForStudent(String courseCode, String studentId) {
        if (courseCode == null || studentId == null) return List.of();
        if (!enrollmentService.isStudentRegisteredForCourse(studentId, courseCode))
            return List.of();
        return dao.listByCourse(courseCode);
    }

    public Document getById(String id) {
        if (id == null || id.isBlank()) return null;
        return dao.findById(id);
    }

    public Document getByIdForStudent(String id, String studentId) {
        if (id == null || studentId == null) return null;

        Document a = dao.findById(id);
        if (a == null) return null;

        String courseCode = a.getString("courseCode");
        if (courseCode == null) return null;

        if (enrollmentService.isStudentRegisteredForCourse(studentId, courseCode))
            return a;

        return null;
    }

   
    public boolean update(String id, Document updates) {
        if (id == null || updates == null) return false;
        return dao.update(id, updates);
    }

   
    public boolean delete(String id) {
        if (id == null) return false;
        return dao.delete(id);
    }

   
    public Document uploadAttachmentFromPath(String filePath, String filename, String contentType)
            throws IOException {

        try (InputStream is = new FileInputStream(filePath)) {
            String fileId = FileStorageUtil.uploadFile(
                    FileStorageUtil.safeFilename(filename),
                    is,
                    contentType
            );

            return new Document()
                    .append("filename", filename)
                    .append("storageRef", fileId)
                    .append("contentType", contentType);
        }
    }

    
    public String getSubjectIdFromAssignment(Document a) {
        if (a == null) return null;

      
        String courseCode = a.getString("courseCode");
        if (courseCode != null && !courseCode.isBlank()) return courseCode;

        
        Object id = a.get("_id");
        if (id != null) return "course::" + id.toString();

        return null;
    }
}
