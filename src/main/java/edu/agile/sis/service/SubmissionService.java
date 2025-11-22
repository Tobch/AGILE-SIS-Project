package edu.agile.sis.service;

import edu.agile.sis.dao.SubmissionDAO;
import edu.agile.sis.util.FileStorageUtil;
import org.bson.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * SubmissionService - handles student submissions, downloads, grading.
 */
public class SubmissionService {
    private final SubmissionDAO dao = new SubmissionDAO();
    private final AssignmentService assignmentService = new AssignmentService();

    public SubmissionService() {}

    public String submit(String assignmentIdHex, String studentId, List<Document> attachments, Document answers) {
        Document assignment = assignmentService.getById(assignmentIdHex);
        if (assignment == null) throw new IllegalArgumentException("Assignment not found");

        Date now = new Date();
        Document sub = new Document("assignmentId", assignmentIdHex)
                .append("studentId", studentId)
                .append("submittedAt", now)
                .append("files", attachments == null ? List.of() : attachments)
                .append("answers", answers == null ? new Document() : answers)
                .append("status", "submitted")
                .append("grade", null)
                .append("feedback", null);

        return dao.insertSubmission(sub);
    }

    public List<Document> listByAssignment(String assignmentIdHex) {
        return dao.listByAssignment(assignmentIdHex);
    }

    public List<Document> listByStudent(String studentId) {
        return dao.listByStudent(studentId);
    }

    public boolean gradeSubmission(String submissionIdHex, double grade, String feedback, String grader) {
        Document updates = new Document("grade", grade)
                .append("feedback", feedback)
                .append("gradedAt", new Date())
                .append("grader", grader)
                .append("status", "graded");
        return dao.update(submissionIdHex, updates);
    }

    public Document getById(String submissionId) { return dao.findById(submissionId); }

    /**
     * Upload attachment and return reference document.
     */
    public Document uploadAttachmentFromPath(String filePath, String filename, String contentType) throws IOException {
        try (InputStream is = new FileInputStream(filePath)) {
            String fileId = FileStorageUtil.uploadFile(FileStorageUtil.safeFilename(filename), is, contentType);
            return new Document("filename", filename).append("storageRef", fileId);
        }
    }

    /**
     * Returns the FIRST submission document for the given student and assignment (or null if none).
     * This is a convenience method; your app may allow multiple submissions per student; adjust as needed.
     */
    public Document getSubmissionForStudent(String assignmentIdHex, String studentEntityId) {
        if (assignmentIdHex == null || studentEntityId == null) return null;
        List<Document> subs = listByAssignment(assignmentIdHex);
        if (subs == null || subs.isEmpty()) return null;
        for (Document s : subs) {
            Object sid = s.get("studentId");
            if (sid != null && studentEntityId.equals(sid.toString())) {
                return s;
            }
        }
        // fallback: check by student listing
        List<Document> byStudent = listByStudent(studentEntityId);
        if (byStudent == null) return null;
        for (Document s : byStudent) {
            Object aid = s.get("assignmentId");
            if (aid == null) continue;
            String aidStr = aid.toString();
            if (aidStr.equals(assignmentIdHex)) return s;
        }
        return null;
    }
}
