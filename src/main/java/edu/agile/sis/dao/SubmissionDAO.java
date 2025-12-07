package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;


public class SubmissionDAO {
    private final MongoCollection<Document> coll;

    public SubmissionDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("submissions");
    }

    public String insertSubmission(Document doc) {
        coll.insertOne(doc);
        Object id = doc.get("_id");
        if (id instanceof ObjectId) return ((ObjectId) id).toHexString();
        return id == null ? null : id.toString();
    }

    
    public List<Document> listByAssignment(String assignmentIdHex) {
        if (assignmentIdHex == null) return new ArrayList<>();
        List<Document> result = new ArrayList<>();
        try {
            
            ObjectId oid = new ObjectId(assignmentIdHex);
            result = coll.find(Filters.eq("assignmentId", oid))
                    .sort(Sorts.ascending("submittedAt"))
                    .into(new ArrayList<>());
        } catch (IllegalArgumentException ex) {
         
        }

        
        if (result.isEmpty()) {
            result = coll.find(Filters.eq("assignmentId", assignmentIdHex))
                    .sort(Sorts.ascending("submittedAt"))
                    .into(new ArrayList<>());
        }

        
        if (result.isEmpty()) {
           
            List<Document> fallback = new ArrayList<>();
            try {
                ObjectId oid2 = new ObjectId(assignmentIdHex);
                fallback = coll.find(Filters.or(
                        Filters.eq("assignmentId", assignmentIdHex),
                        Filters.eq("assignmentId", oid2)
                )).sort(Sorts.ascending("submittedAt")).into(new ArrayList<>());
            } catch (IllegalArgumentException ignored) {
                
            }
            if (!fallback.isEmpty()) result = fallback;
        }

        
        System.out.println("[SubmissionDAO] listByAssignment -> assignmentId=" + assignmentIdHex + " -> found=" + (result == null ? 0 : result.size()));

        return result == null ? new ArrayList<>() : result;
    }

    public List<Document> listByStudent(String studentId) {
    if (studentId == null) return new ArrayList<>();
    List<Document> result = new ArrayList<>();

  
    try {
        ObjectId oid = new ObjectId(studentId);
        result = coll.find(Filters.eq("studentId", oid))
                .sort(Sorts.ascending("submittedAt"))
                .into(new ArrayList<>());
    } catch (IllegalArgumentException ignored) {

    }

  
    if (result.isEmpty()) {
        result = coll.find(Filters.eq("studentId", studentId))
                .sort(Sorts.ascending("submittedAt"))
                .into(new ArrayList<>());
    }


    if (result.isEmpty()) {
        try {
            ObjectId oid2 = new ObjectId(studentId);
            result = coll.find(Filters.or(
                    Filters.eq("studentId", studentId),
                    Filters.eq("studentId", oid2)
            )).sort(Sorts.ascending("submittedAt")).into(new ArrayList<>());
        } catch (IllegalArgumentException ignored) {
        }
    }

    return result == null ? new ArrayList<>() : result;
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
