package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO {
    private final MongoCollection<Document> coll;

    public AssignmentDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("assignments");
    }

    public String insertAssignment(Document doc) {
        coll.insertOne(doc);
        Object id = doc.get("_id");
        if (id instanceof ObjectId) return ((ObjectId) id).toHexString();
        return id == null ? null : id.toString();
    }

    public List<Document> listByCourse(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return coll.find().into(new ArrayList<>());
        }
        return coll.find(Filters.eq("courseCode", courseCode)).into(new ArrayList<>());
    }

    public Document findById(String hexId) {
        try {
            return coll.find(Filters.eq("_id", new ObjectId(hexId))).first();
        } catch (IllegalArgumentException ex) {
            return coll.find(Filters.eq("_id", hexId)).first();
        }
    }

    public boolean update(String hexId, Document updates) {
        try {
            coll.updateOne(Filters.eq("_id", new ObjectId(hexId)), new Document("$set", updates));
            return true;
        } catch (Exception ex) {
            try {
                coll.updateOne(Filters.eq("_id", hexId), new Document("$set", updates));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public boolean delete(String hexId) {
        try {
            coll.deleteOne(Filters.eq("_id", new ObjectId(hexId)));
            return true;
        } catch (IllegalArgumentException ex) {
            coll.deleteOne(Filters.eq("_id", hexId));
            return true;
        }
    }
}
