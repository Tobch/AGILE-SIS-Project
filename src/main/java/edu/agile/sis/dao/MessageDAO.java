package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessageDAO {
    private final MongoCollection<Document> coll;

    public MessageDAO(){
        this.coll = DBConnection.getInstance().getDatabase().getCollection("messages");
    }

    public void insertMessage(Document msg){
        msg.append("createdAt", new Date());
        msg.append("read", false);
        coll.insertOne(msg);
    }

    public List<Document> findThreadBetween(String studentId, String staffId){
        return coll.find(Filters.and(
                Filters.eq("studentId", studentId),
                Filters.eq("staffId", staffId)
        )).sort(Sorts.ascending("createdAt")).into(new ArrayList<>());
    }

    public List<Document> findByStudent(String studentId){
        return coll.find(Filters.eq("studentId", studentId)).sort(Sorts.descending("createdAt")).into(new ArrayList<>());
    }

    public List<Document> findByStaff(String staffId){
        return coll.find(Filters.eq("staffId", staffId)).sort(Sorts.descending("createdAt")).into(new ArrayList<>());
    }

    public void markRead(String messageId){
        try {
            coll.updateOne(Filters.eq("_id", new ObjectId(messageId)), new Document("$set", new Document("read", true)));
        } catch (IllegalArgumentException ex){
            coll.updateOne(Filters.eq("_id", messageId), new Document("$set", new Document("read", true)));
        }
    }

    public void delete(String messageId){
        try {
            coll.deleteOne(Filters.eq("_id", new ObjectId(messageId)));
        } catch (IllegalArgumentException ex){
            coll.deleteOne(Filters.eq("_id", messageId));
        }
    }
}
