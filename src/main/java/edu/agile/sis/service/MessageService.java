package edu.agile.sis.service;

import edu.agile.sis.dao.MessageDAO;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;


public class MessageService {

    private final MessageDAO dao = new MessageDAO();
    private final ConversationService convService = new ConversationService();

    
    public void sendMessage(String studentId, String staffId, String senderId, String body) {
        if (studentId == null || studentId.isBlank() ||
                staffId == null || staffId.isBlank() ||
                senderId == null || senderId.isBlank() ||
                body == null || body.isBlank()) {
            throw new IllegalArgumentException("Invalid message parameters.");
        }

        
        String threadId = convService.buildThreadId(studentId, staffId);

        Document doc = new Document()
                .append("studentId", studentId)
                .append("staffId", staffId)
                .append("senderId", senderId)
                .append("receiverId", resolveReceiver(studentId, staffId, senderId))
                .append("threadId", threadId)
                .append("body", body)
                .append("createdAt", new Date())
                .append("read", false);

        dao.insertMessage(doc);
    }

   
    private String resolveReceiver(String studentId, String staffId, String senderId) {
        if (senderId.equals(staffId)) return studentId;
        return staffId;
    }

    public List<Document> getThread(String studentId, String staffId) {
        if (studentId == null || studentId.isBlank() || staffId == null || staffId.isBlank()) {
            return Collections.emptyList();
        }

        
        String threadId = convService.buildThreadId(studentId, staffId);
        List<Document> raw;
        try {
            raw = convService.getMessagesForThread(threadId, studentId, staffId);
        } catch (Exception ex) {
            
            raw = dao.findThreadBetween(studentId, staffId);
        }

        if (raw == null || raw.isEmpty()) return Collections.emptyList();

      
        LinkedHashMap<String, Document> seen = new LinkedHashMap<>();
        for (Document d : raw) {
            String msgStudent = safeStr(d.getString("studentId"));
            if (!studentId.equals(msgStudent)) continue;

            Object rawId = d.get("_id");
            String key = (rawId != null) ? rawId.toString()
                    : (safeStr(d.getString("senderId")) + "|" + safeStr(d.getString("receiverId")) + "|" +
                    (d.getDate("createdAt") == null ? "0" : Long.toString(d.getDate("createdAt").getTime())) + "|" +
                    safeStr(d.getString("body")));

            if (!seen.containsKey(key)) seen.put(key, d);
        }

        List<Document> out = new ArrayList<>(seen.values());
        out.sort(Comparator.comparing(d -> Optional.ofNullable(d.getDate("createdAt")).orElse(new Date(0L))));
        return out;
    }

    
    public List<Document> getByStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) return Collections.emptyList();

        List<Document> raw = dao.findByStudent(studentId);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        LinkedHashMap<String, Document> seen = new LinkedHashMap<>();
        for (Document d : raw) {
            String msgStudent = safeStr(d.getString("studentId"));
            if (!studentId.equals(msgStudent)) continue;

            Object rawId = d.get("_id");
            String key = (rawId != null) ? rawId.toString()
                    : (safeStr(d.getString("senderId")) + "|" + safeStr(d.getString("receiverId")) + "|" +
                    (d.getDate("createdAt") == null ? "0" : Long.toString(d.getDate("createdAt").getTime())) + "|" +
                    safeStr(d.getString("body")));

            if (!seen.containsKey(key)) seen.put(key, d);
        }

        List<Document> out = new ArrayList<>(seen.values());
        out.sort(Comparator.comparing(d -> Optional.ofNullable(d.getDate("createdAt")).orElse(new Date(0L))));
        return out;
    }

public List<Document> getByStaff(String staffId) {
    if (staffId == null || staffId.isBlank()) return Collections.emptyList();

    
    List<Document> rawAgg = Collections.emptyList();
    try {
        rawAgg = dao.findByStaffLatestPartnerMessages(staffId);
    } catch (Throwable t) {
   
        rawAgg = Collections.emptyList();
    }

    List<Document> use = rawAgg;
    
    if (use == null || use.isEmpty()) {
        try {
            use = dao.findByStaff(staffId);
        } catch (Throwable t) {
            use = Collections.emptyList();
        }
    }

    if (use == null || use.isEmpty()) return Collections.emptyList();

    
    LinkedHashMap<String, Document> seen = new LinkedHashMap<>();
    for (Document d : use) {
        String msgStaff = safeStr(d.getString("staffId"));
        
        if (!msgStaff.isBlank() && !staffId.equals(msgStaff)) {
   
            continue;
        }

        Object rawId = d.get("_id");
        String key = (rawId != null) ? rawId.toString()
                : (safeStr(d.getString("senderId")) + "|" + safeStr(d.getString("receiverId")) + "|" +
                   (d.getDate("createdAt") == null ? "0" : Long.toString(d.getDate("createdAt").getTime())) + "|" +
                   safeStr(d.getString("body")));

        if (!seen.containsKey(key)) seen.put(key, d);
    }

    List<Document> out = new ArrayList<>(seen.values());
   
    out.sort((a, b) -> {
        Date da = a.getDate("createdAt");
        Date db = b.getDate("createdAt");
        long ta = da == null ? 0L : da.getTime();
        long tb = db == null ? 0L : db.getTime();
        return Long.compare(tb, ta);
    });
    return out;
}


    public void markRead(String messageId){ dao.markRead(messageId); }

    public void delete(String messageId){ dao.delete(messageId); }

    private static String safeStr(Object o) {
        return (o == null) ? "" : o.toString();
    }
}
