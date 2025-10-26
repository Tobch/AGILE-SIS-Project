package edu.agile.sis.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Generic entity service for documents stored in collections such as "students", "staff", "courses", etc.
 * Provides safe merge update for nested `core` document and attributes array.
 */
public class EntityService {
    private final MongoCollection<Document> coll;

    public EntityService(String collectionName) {
        this.coll = DBConnection.getInstance().getDatabase().getCollection(collectionName);
    }

    public Document getEntityById(String entityId) {
        if (entityId == null) return null;
        // try matching core.entityId or top-level entityId or _id (string form)
        Document d = coll.find(Filters.or(
                Filters.eq("core.entityId", entityId),
                Filters.eq("entityId", entityId),
                Filters.eq("_id", entityId)
        )).first();
        return d;
    }

    public List<Document> getEntitiesByType(String typeName) {
        return coll.find(Filters.eq("type", typeName)).into(new ArrayList<>());
    }

    public boolean createEntity(Document doc) {
        if (doc == null) return false;
        coll.insertOne(doc);
        return true;
    }

    public boolean deleteEntity(String entityId) {
        Document existing = getEntityById(entityId);
        if (existing == null) return false;
        coll.deleteOne(Filters.eq("_id", existing.get("_id")));
        return true;
    }

    /**
     * Safely merge updates into existing entity's `core` document and/or replace attributes.
     *
     * - coreUpdates: map of keys to values that should be applied into core (only provided keys change)
     * - attributes: if non-null, replaces entire attributes array with the provided list
     *
     * This method fetches the existing doc, merges core values, and writes back full `core` to avoid accidentally
     * deleting fields such as enrolledSince.
     */
    public boolean updateEntityMerge(String entityId, Map<String, Object> coreUpdates, List<Document> attributes) {
        Document existing = getEntityById(entityId);
        if (existing == null) return false;

        Document core = existing.get("core", Document.class);
        if (core == null) core = new Document();

        if (coreUpdates != null) {
            for (Map.Entry<String, Object> e : coreUpdates.entrySet()) {
                core.put(e.getKey(), e.getValue());
            }
        }

        Document setDoc = new Document();
        setDoc.append("core", core);
        if (attributes != null) setDoc.append("attributes", attributes);
        setDoc.append("updatedAt", new Date());

        UpdateResult res = coll.updateOne(Filters.eq("_id", existing.get("_id")), new Document("$set", setDoc));
        return res.getModifiedCount() > 0;
    }

    /**
     * Convenience: replace attributes only, preserves core.
     */
    public boolean replaceAttributes(String entityId, List<Document> attributes) {
        return updateEntityMerge(entityId, null, attributes);
    }

    /**
     * Convenience: update individual core field (single field)
     */
    public boolean updateCoreField(String entityId, String key, Object value) {
        return updateEntityMerge(entityId, Map.of(key, value), null);
    }
}
