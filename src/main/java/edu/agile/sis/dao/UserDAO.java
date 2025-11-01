package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final MongoCollection<Document> users;

    public UserDAO() {
        this.users = DBConnection.getInstance().getDatabase().getCollection("users");

        // ✅ Ensure unique index on linkedEntityId (prevents duplicates)
        try {
            users.createIndex(new Document("linkedEntityId", 1), new IndexOptions().unique(true));
        } catch (Exception ignored) {
            // Index already exists or MongoDB may skip if already unique
        }
    }

    /**
     * Inserts a new user if no duplicate username or linkedEntityId exists.
     * Returns false if duplicate found.
     */
    public boolean insertUser(String username, String passwordHash, List<String> roles, String linkedEntityId) {
        // Check for duplicates (username OR linkedEntityId)
        Document existing = users.find(Filters.or(
                Filters.eq("username", username),
                Filters.eq("linkedEntityId", linkedEntityId)
        )).first();

        if (existing != null) {
            return false; // prevent duplicate user
        }

        Document doc = new Document("username", username)
                .append("passwordHash", passwordHash)
                .append("roles", roles)
                .append("linkedEntityId", linkedEntityId)
                .append("createdAt", new java.util.Date());
        users.insertOne(doc);
        return true;
    }

    public Document findByUsername(String username) {
        return users.find(Filters.eq("username", username)).first();
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

    /**
     * Updates username and roles for a user linked to a specific entity ID.
     * Returns true if a record was updated.
     */
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
}
