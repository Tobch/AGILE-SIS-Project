package edu.agile.sis.dao;

import edu.agile.sis.db.DBConnection;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ConversationDAO {

    private final MongoCollection<Document> coll;

    public ConversationDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("messages");
    }

   
    public List<Document> listThreadPreviewsForStaff(String staffId) {
        if (staffId == null || staffId.isBlank()) return new ArrayList<>();

       
        List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document("staffId", staffId)),
                
                new Document("$group", new Document("_id",
                        new Document("threadId",
                                new Document("$ifNull", Arrays.asList("$threadId",
                                        new Document("$concat", Arrays.asList(
                                                "student:", new Document("$ifNull", Arrays.asList("$studentId", "")),
                                                "|staff:", "$staffId"
                                        ))
                                ))
                        ))
                        .append("latest", new Document("$last", "$$ROOT"))
                ),
               
                new Document("$project", new Document("threadId", "$_id.threadId")
                        .append("latest", "$latest")
                        .append("studentId", "$latest.studentId")
                        .append("staffId", "$latest.staffId")
                        .append("latestAt", "$latest.createdAt")
                ),
                new Document("$sort", new Document("latestAt", -1))
        );

        List<Document> out = new ArrayList<>();
        coll.aggregate(pipeline).into(out);
        return out;
    }


    public List<Document> listThreadPreviewsForParent(List<String> parentStudentIds) {
        if (parentStudentIds == null || parentStudentIds.isEmpty()) return new ArrayList<>();

        List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document("studentId", new Document("$in", parentStudentIds))),
                new Document("$group", new Document("_id",
                        new Document("threadId",
                                new Document("$ifNull", Arrays.asList("$threadId",
                                        new Document("$concat", Arrays.asList(
                                                "student:", "$studentId",
                                                "|staff:", "$staffId"
                                        ))
                                ))
                        ))
                        .append("latest", new Document("$last", "$$ROOT"))
                ),
                new Document("$project", new Document("threadId", "$_id.threadId")
                        .append("latest", "$latest")
                        .append("studentId", "$latest.studentId")
                        .append("staffId", "$latest.staffId")
                        .append("latestAt", "$latest.createdAt")
                ),
                new Document("$sort", new Document("latestAt", -1))
        );

        List<Document> out = new ArrayList<>();
        coll.aggregate(pipeline).into(out);
        return out;
    }

    
    public List<Document> findMessagesForThread(String threadId, String studentId, String staffId) {
        List<Document> out = new ArrayList<>();

        if (threadId != null && !threadId.isBlank()) {
            coll.find(new Document("threadId", threadId))
                    .sort(new Document("createdAt", 1))
                    .into(out);
            return out;
        }

        if (studentId != null && staffId != null) {
            coll.find(new Document("studentId", studentId).append("staffId", staffId))
                    .sort(new Document("createdAt", 1))
                    .into(out);
            return out;
        }

        return out;
    }

  
    public static String buildThreadId(String studentId, String staffId) {
        if (studentId == null) studentId = "";
        if (staffId == null) staffId = "";
        return "student:" + studentId + "|staff:" + staffId;
    }
}
