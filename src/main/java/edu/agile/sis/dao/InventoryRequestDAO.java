package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for inventory requests collection.
 */
public class InventoryRequestDAO {
    private final MongoCollection<Document> requests;

    public InventoryRequestDAO() {
        this.requests = DBConnection.getInstance().getDatabase().getCollection("inventory_requests");
    }

    public ObjectId insertRequest(Document request) {
        requests.insertOne(request);
        return request.getObjectId("_id");
    }

    public List<Document> findAllRequests() {
        return requests.find()
                .sort(Sorts.descending("requestDate"))
                .into(new ArrayList<>());
    }

    public Document findById(String requestId) {
        ObjectId oid = tryParseObjectId(requestId);
        if (oid == null) {
            return requests.find(Filters.eq("_id", requestId)).first();
        }
        return requests.find(Filters.eq("_id", oid)).first();
    }

    public List<Document> findByStatus(String status) {
        return requests.find(Filters.eq("status", status))
                .sort(Sorts.descending("requestDate"))
                .into(new ArrayList<>());
    }

    public List<Document> findByRequester(String requesterId) {
        return requests.find(Filters.eq("requesterId", requesterId))
                .sort(Sorts.descending("requestDate"))
                .into(new ArrayList<>());
    }

    public List<Document> findByItem(String itemId) {
        return requests.find(Filters.eq("itemId", itemId))
                .sort(Sorts.descending("requestDate"))
                .into(new ArrayList<>());
    }

    /**
     * Find pending requests for a specific item (to check if user already has
     * pending request).
     */
    public Document findPendingByRequesterAndItem(String requesterId, String itemId) {
        return requests.find(Filters.and(
                Filters.eq("requesterId", requesterId),
                Filters.eq("itemId", itemId),
                Filters.eq("status", "Pending"))).first();
    }

    public UpdateResult updateRequest(String requestId, Document updatedData) {
        ObjectId oid = tryParseObjectId(requestId);
        if (oid == null) {
            return requests.updateOne(Filters.eq("_id", requestId), new Document("$set", updatedData));
        }
        return requests.updateOne(Filters.eq("_id", oid), new Document("$set", updatedData));
    }

    public void deleteRequest(String requestId) {
        ObjectId oid = tryParseObjectId(requestId);
        if (oid == null) {
            requests.deleteOne(Filters.eq("_id", requestId));
        } else {
            requests.deleteOne(Filters.eq("_id", oid));
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
