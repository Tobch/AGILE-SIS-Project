package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RSVPDAO {
    private final MongoCollection<Document> coll;

    public RSVPDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("rsvps");
    }

    public String insert(String eventId, String studentId, String username) {
        Document doc = new Document("eventId", tryObjectId(eventId))
                .append("studentId", studentId)
                .append("username", username)
                .append("status", "attending")
                .append("createdAt", new Date());
        coll.insertOne(doc);
        Object id = doc.get("_id");
        return id == null ? null : id.toString();
    }

    public List<Document> listByEvent(String eventId) {
        Object key = tryObjectId(eventId);
        return coll.find(Filters.eq("eventId", key)).into(new ArrayList<>());
    }

    public void cancel(String rsvpId) {
        try { coll.deleteOne(Filters.eq("_id", new ObjectId(rsvpId))); }
        catch (Exception e) { coll.deleteOne(Filters.eq("_id", rsvpId)); }
    }

    private Object tryObjectId(String hex) {
        try { return new ObjectId(hex); } catch (Exception ex) { return hex; }
    }
}
