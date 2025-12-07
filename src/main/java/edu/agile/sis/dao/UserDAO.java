package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDAO {
    private final MongoCollection<Document> users;

    public UserDAO() {
        this.users = DBConnection.getInstance().getDatabase().getCollection("users");

        
        try {
            users.createIndex(new Document("linkedEntityId", 1), new IndexOptions().unique(true));
        } catch (Exception ignored) {
            
        }
    }

    
    public boolean insertUser(String username, String passwordHash, List<String> roles, String linkedEntityId) {
        if (username == null || username.isBlank()) return false;

       
        String normalizedUsername = username.trim();

        
        Document existing = users.find(Filters.or(
                Filters.eq("username", normalizedUsername),
                Filters.eq("linkedEntityId", linkedEntityId)
        )).first();

        if (existing != null) {
            return false; 
        }

        Document doc = new Document("username", normalizedUsername)
                .append("passwordHash", passwordHash)
                .append("roles", roles)
                .append("linkedEntityId", linkedEntityId)
                .append("createdAt", new java.util.Date());
        users.insertOne(doc);
        return true;
    }

    public Document findByUsername(String username) {
        if (username == null) return null;
        return users.find(Filters.eq("username", username)).first();
    }

   
    public Document findByLinkedEntityId(String linkedEntityId) {
        if (linkedEntityId == null || linkedEntityId.isBlank()) return null;
        return users.find(Filters.eq("linkedEntityId", linkedEntityId)).first();
    }

    public List<Document> findAllUsers() {
        return users.find().into(new ArrayList<>());
    }

    public void updatePassword(String username, String newHash) {
        users.updateOne(Filters.eq("username", username),
                new Document("$set", new Document("passwordHash", newHash)));
    }

    public void deleteUser(String username) {
        users.deleteOne(Filters.eq("username", username));
    }

    public boolean deleteByLinkedEntityId(String linkedEntityId) {
        if (linkedEntityId == null || linkedEntityId.isBlank()) return false;
        var result = users.deleteOne(Filters.eq("linkedEntityId", linkedEntityId));
        return result.getDeletedCount() > 0;
    }

   
    public boolean updateUserByLinkedEntityId(String linkedEntityId, String newUsername, List<String> newRoles) {
        if (linkedEntityId == null || linkedEntityId.isBlank()) return false;

        Document updateDoc = new Document();
        if (newUsername != null && !newUsername.isBlank()) {
            updateDoc.append("username", newUsername.trim());
        }
        if (newRoles != null && !newRoles.isEmpty()) {
            updateDoc.append("roles", newRoles);
        }

        if (updateDoc.isEmpty()) return false;

        var result = users.updateOne(Filters.eq("linkedEntityId", linkedEntityId),
                new Document("$set", updateDoc)
                        .append("$currentDate", new Document("updatedAt", true)));

        return result.getModifiedCount() > 0;
    }

    
    public List<Document> getStudentsForParent(String parentId) {
        if (parentId == null || parentId.isBlank()) return new ArrayList<>();

        return users.find(Filters.or(
                Filters.eq("parentId", parentId),
                Filters.eq("parentLinkedEntityId", parentId)
        )).into(new ArrayList<>());
    }
    
    
    public Map<String, Document> findByLinkedEntityIds(List<String> linkedIds) {
    if (linkedIds == null || linkedIds.isEmpty()) return Collections.emptyMap();
    Map<String, Document> out = new HashMap<>();
    try (var cursor = users.find(Filters.in("linkedEntityId", linkedIds)).iterator()) {
        while (cursor.hasNext()) {
            Document d = cursor.next();
            String lid = d.getString("linkedEntityId");
            if (lid != null) out.put(lid, d);
        }
    } catch (Throwable t) {
        t.printStackTrace();
    }
    return out;
}
    
    
    
    
    
    
    
    
   
public int countStudentsWithParent(String parentEntityId) {
    if (parentEntityId == null || parentEntityId.isBlank()) return 0;
    try {
        return (int) users.countDocuments(Filters.eq("parentLinkedEntityId", parentEntityId));
    } catch (Throwable t) {
        t.printStackTrace();
        return 0;
    }
}


public long unlinkParentFromStudents(String parentEntityId) {
    if (parentEntityId == null || parentEntityId.isBlank()) return 0L;
    try {
        var res = users.updateMany(
                Filters.eq("parentLinkedEntityId", parentEntityId),
                new Document("$unset", new Document("parentLinkedEntityId", ""))
                        .append("$currentDate", new Document("updatedAt", true))
        );
        return res.getModifiedCount();
    } catch (Throwable t) {
        t.printStackTrace();
        return 0L;
    }
}


public List<String> getStudentIdsForParent(String parentEntityId) {
    List<String> out = new ArrayList<>();
    if (parentEntityId == null || parentEntityId.isBlank()) return out;
    try (var cursor = users.find(Filters.eq("parentLinkedEntityId", parentEntityId)).iterator()) {
        while (cursor.hasNext()) {
            Document d = cursor.next();
            String linked = d.getString("linkedEntityId");
            if (linked != null && !linked.isBlank()) {
                out.add(linked);
            } else {
                ObjectId oid = d.getObjectId("_id");
                if (oid != null) out.add(oid.toHexString());
            }
        }
    } catch (Throwable t) {
        t.printStackTrace();
    }
    return out;
}


    
    

    public boolean setParentForStudent(String studentId, String parentLinkedEntityId) {
        if (studentId == null || studentId.isBlank() || parentLinkedEntityId == null || parentLinkedEntityId.isBlank())
            return false;

       
        try {
            UpdateResult res = users.updateOne(Filters.eq("_id", new ObjectId(studentId)),
                    new Document("$set", new Document("parentLinkedEntityId", parentLinkedEntityId)
                            .append("updatedAt", new java.util.Date())));
            if (res.getModifiedCount() > 0) return true;
        } catch (IllegalArgumentException ignored) {
            
        }

    
        UpdateResult res2 = users.updateOne(Filters.or(
                Filters.eq("linkedEntityId", studentId),
                Filters.eq("_id", studentId)
        ), new Document("$set", new Document("parentLinkedEntityId", parentLinkedEntityId)
                .append("updatedAt", new java.util.Date())));

        return res2.getModifiedCount() > 0;
    }
}
