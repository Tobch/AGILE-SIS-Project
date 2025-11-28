package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;


public class LeaveDAO {

    private final MongoCollection<Document> col =
            DBConnection.getInstance().getDatabase().getCollection("leaves");

 
    public Document insert(Document d) {
        col.insertOne(d);
        return d;
    }


    public List<Document> findAll() {
        List<Document> out = new ArrayList<>();
        col.find().sort(Sorts.descending("createdAt")).into(out);
        return out;
    }


    public List<Document> findByStaffId(String staffId) {
        List<Document> out = new ArrayList<>();
        col.find(Filters.eq("staffId", staffId)).sort(Sorts.descending("createdAt")).into(out);
        return out;
    }


    public Document findById(String id) {
        try {
            return col.find(Filters.eq("_id", new ObjectId(id))).first();
        } catch (Exception ex) {
            return null;
        }
    }

 
    public boolean update(String id, Document updated) {
        try {
            ObjectId oid = new ObjectId(id);
            Document setDoc = new Document();
            for (String k : updated.keySet()) {
                if ("_id".equals(k)) continue;
                setDoc.append(k, updated.get(k));
            }
            if (setDoc.isEmpty()) return false;
            Document update = new Document("$set", setDoc);
            return col.updateOne(Filters.eq("_id", oid), update).getModifiedCount() > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }


    public boolean updateStatus(String id, String status, String approver, String note) {
        try {
            ObjectId oid = new ObjectId(id);
            List<org.bson.conversions.Bson> updates = new ArrayList<>();
            updates.add(Updates.set("status", status));
            if (approver != null) updates.add(Updates.set("approver", approver));
            if (note != null) updates.add(Updates.set("approverNote", note));
            updates.add(Updates.set("updatedAt", System.currentTimeMillis()));
            org.bson.conversions.Bson u = Updates.combine(updates);
            return col.updateOne(Filters.eq("_id", oid), u).getModifiedCount() > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }


    public boolean deleteById(String id) {
        try {
            ObjectId oid = new ObjectId(id);
            return col.deleteOne(Filters.eq("_id", oid)).getDeletedCount() > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
