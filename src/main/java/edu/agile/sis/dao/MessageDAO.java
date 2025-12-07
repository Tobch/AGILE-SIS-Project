package edu.agile.sis.dao;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class MessageDAO {

    private final MongoCollection<Document> coll;

    public MessageDAO(){
        this.coll = DBConnection.getInstance()
                .getDatabase()
                .getCollection("messages");
    }

    public void insertMessage(Document msg){
        if (!msg.containsKey("createdAt"))
            msg.append("createdAt", new Date());

        msg.append("read", false);
        coll.insertOne(msg);
    }

    
    public List<Document> findByThreadId(String threadId){
        if (threadId == null || threadId.isBlank()) return new ArrayList<>();
        return coll.find(Filters.eq("threadId", threadId))
                .sort(Sorts.ascending("createdAt"))
                .into(new ArrayList<>());
    }

    
    public List<Document> findThreadBetween(String studentId, String staffId){
        if (studentId == null || staffId == null) return new ArrayList<>();

        
        try {
            
            String threadId = "student:" + studentId + "|staff:" + staffId; 
            List<Document> byThread = findByThreadId(threadId);
            if (byThread != null && !byThread.isEmpty()) return byThread;
        } catch (Exception ignored) {
            
        }

        
        Bson studentFilter = Filters.eq("studentId", studentId);

        try {
            ObjectId oid = new ObjectId(studentId);
            studentFilter = Filters.or(Filters.eq("studentId", studentId), Filters.eq("studentId", oid.toHexString()));
        } catch (IllegalArgumentException ignore) {
            
        }

        return coll.find(Filters.and(studentFilter, Filters.eq("staffId", staffId)))
                .sort(Sorts.ascending("createdAt"))
                .into(new ArrayList<>());
    }

    
    public List<Document> findByStaff(String staffId){
        if (staffId == null || staffId.isBlank()) return new ArrayList<>();
        return coll.find(Filters.eq("staffId", staffId))
                .sort(Sorts.descending("createdAt"))
                .into(new ArrayList<>());
    }

    
    public List<Document> findByStudent(String studentId){
        if (studentId == null || studentId.isBlank()) return new ArrayList<>();
        return coll.find(Filters.eq("studentId", studentId))
                .sort(Sorts.descending("createdAt"))
                .into(new ArrayList<>());
    }

   
    public List<Document> findByStaffLatestPartnerMessages(String staffId){
        if (staffId == null || staffId.isBlank()) return new ArrayList<>();

        
        Document match = new Document("$match", new Document("staffId", staffId));

   
        Document cond = new Document("$cond", Arrays.asList(
                new Document("$and", Arrays.asList(
                        new Document("$ne", Arrays.asList("$senderId", staffId)),
                        new Document("$ne", Arrays.asList("$senderId", "$studentId"))
                )),
                "$senderId",
                "$studentId"
        ));
        Document addFields = new Document("$addFields", new Document("partner", cond));

        
        Document sort = new Document("$sort", new Document("createdAt", -1));

       
        Document group = new Document("$group", new Document("_id", "$partner")
                .append("doc", new Document("$first", "$$ROOT")));

       
        Document replaceRoot = new Document("$replaceRoot", new Document("newRoot", "$doc"));

      
        Document finalSort = new Document("$sort", new Document("createdAt", -1));

        List<Bson> pipeline = Arrays.asList(
                match,
                addFields,
                sort,
                group,
                replaceRoot,
                finalSort
        );

        AggregateIterable<Document> agg = coll.aggregate(pipeline);
        List<Document> out = new ArrayList<>();
        for (Document d : agg) out.add(d);
        return out;
    }

    public void markRead(String messageId){
        if (messageId == null || messageId.isBlank()) return;
        try {
            coll.updateOne(Filters.eq("_id", new ObjectId(messageId)),
                    new Document("$set", new Document("read", true)));
        } catch (Exception ex){
            coll.updateOne(Filters.eq("_id", messageId),
                    new Document("$set", new Document("read", true)));
        }
    }

    public void delete(String messageId){
        if (messageId == null || messageId.isBlank()) return;
        try {
            coll.deleteOne(Filters.eq("_id", new ObjectId(messageId)));
        } catch (Exception ex){
            coll.deleteOne(Filters.eq("_id", messageId));
        }
    }
}
