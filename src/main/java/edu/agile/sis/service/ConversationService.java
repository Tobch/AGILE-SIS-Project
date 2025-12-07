package edu.agile.sis.service;

import edu.agile.sis.dao.ConversationDAO;
import org.bson.Document;

import java.util.List;


public class ConversationService {

    private final ConversationDAO dao = new ConversationDAO();

    public List<Document> listThreadsForStaff(String staffId) {
        if (staffId == null || staffId.isBlank()) return List.of();
        try {
            return dao.listThreadPreviewsForStaff(staffId);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load threads for staff: " + t.getMessage(), t);
        }
    }

    public List<Document> listThreadsForParent(List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return List.of();
        try {
            return dao.listThreadPreviewsForParent(studentIds);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load threads for parent: " + t.getMessage(), t);
        }
    }

    public List<Document> getMessagesForThread(String threadId, String studentId, String staffId) {
        try {
            return dao.findMessagesForThread(threadId, studentId, staffId);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load messages for thread: " + t.getMessage(), t);
        }
    }

    
    public String buildThreadId(String studentId, String staffId) {
        return ConversationDAO.buildThreadId(studentId, staffId);
    }
}
