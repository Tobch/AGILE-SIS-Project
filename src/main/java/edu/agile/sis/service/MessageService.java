package edu.agile.sis.service;

import edu.agile.sis.dao.MessageDAO;
import org.bson.Document;

import java.util.List;

public class MessageService {
    private final MessageDAO dao = new MessageDAO();

    public void sendMessage(String studentId, String staffId, String senderId, String body){
        Document doc = new Document("studentId", studentId)
                .append("staffId", staffId)
                .append("senderId", senderId)
                .append("body", body);
        dao.insertMessage(doc);
    }

    public List<Document> getThread(String studentId, String staffId){
        return dao.findThreadBetween(studentId, staffId);
    }

    public List<Document> getByStudent(String studentId){ return dao.findByStudent(studentId); }

    public List<Document> getByStaff(String staffId){ return dao.findByStaff(staffId); }

    public void markRead(String messageId){ dao.markRead(messageId); }

    public void delete(String messageId){ dao.delete(messageId); }
}
