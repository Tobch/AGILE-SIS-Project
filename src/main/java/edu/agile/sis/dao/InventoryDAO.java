package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for inventory items collection.
 */
public class InventoryDAO {
    private final MongoCollection<Document> inventory;

    public InventoryDAO() {
        this.inventory = DBConnection.getInstance().getDatabase().getCollection("inventory");
    }

    public ObjectId insertItem(Document item) {
        inventory.insertOne(item);
        return item.getObjectId("_id");
    }

    public List<Document> findAllItems() {
        return inventory.find().into(new ArrayList<>());
    }

    public Document findById(String itemId) {
        ObjectId oid = tryParseObjectId(itemId);
        if (oid == null) {
            return inventory.find(Filters.eq("_id", itemId)).first();
        }
        return inventory.find(Filters.eq("_id", oid)).first();
    }

    public List<Document> findByStatus(String status) {
        return inventory.find(Filters.eq("status", status)).into(new ArrayList<>());
    }

    public List<Document> findByAssignedUser(String userId) {
        return inventory.find(Filters.eq("assignedToUserId", userId)).into(new ArrayList<>());
    }

    public List<Document> findByItemType(String itemType) {
        return inventory.find(Filters.eq("itemType", itemType)).into(new ArrayList<>());
    }

    /**
     * Find items where the given userId is in the assignedUsers array (for
     * licenses).
     */
    public List<Document> findByUserInAssignedUsers(String userId) {
        return inventory.find(Filters.eq("assignedUsers.userId", userId)).into(new ArrayList<>());
    }

    public UpdateResult updateItem(String itemId, Document updatedData) {
        ObjectId oid = tryParseObjectId(itemId);
        if (oid == null) {
            return inventory.updateOne(Filters.eq("_id", itemId), new Document("$set", updatedData));
        }
        return inventory.updateOne(Filters.eq("_id", oid), new Document("$set", updatedData));
    }

    public void deleteItem(String itemId) {
        ObjectId oid = tryParseObjectId(itemId);
        if (oid == null) {
            inventory.deleteOne(Filters.eq("_id", itemId));
        } else {
            inventory.deleteOne(Filters.eq("_id", oid));
        }
    }

    private ObjectId tryParseObjectId(String id) {
        if (id == null)
            return null;
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
