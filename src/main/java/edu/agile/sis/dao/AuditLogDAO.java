package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for audit logs collection.
 * Records changes to inventory allocations for traceability.
 */
public class AuditLogDAO {
    private final MongoCollection<Document> auditLogs;

    public AuditLogDAO() {
        this.auditLogs = DBConnection.getInstance().getDatabase().getCollection("audit_logs");
    }

    /**
     * Insert an audit log entry.
     * 
     * @param log Document containing: entityType, entityId, action, performedBy,
     *            timestamp, details
     */
    public void insertLog(Document log) {
        if (log.get("timestamp") == null) {
            log.append("timestamp", new Date());
        }
        auditLogs.insertOne(log);
    }

    /**
     * Find all audit logs for a specific entity (e.g., inventory item).
     * 
     * @param entityId The ID of the entity
     * @return List of audit log documents, sorted by timestamp descending
     */
    public List<Document> findByEntityId(String entityId) {
        return auditLogs.find(Filters.eq("entityId", entityId))
                .sort(Sorts.descending("timestamp"))
                .into(new ArrayList<>());
    }

    /**
     * Find all audit logs for a specific entity type.
     * 
     * @param entityType The type of entity (e.g., "inventory")
     * @return List of audit log documents
     */
    public List<Document> findByEntityType(String entityType) {
        return auditLogs.find(Filters.eq("entityType", entityType))
                .sort(Sorts.descending("timestamp"))
                .into(new ArrayList<>());
    }

    /**
     * Find all audit logs performed by a specific user.
     * 
     * @param userId The ID of the user who performed the action
     * @return List of audit log documents
     */
    public List<Document> findByPerformedBy(String userId) {
        return auditLogs.find(Filters.eq("performedBy", userId))
                .sort(Sorts.descending("timestamp"))
                .into(new ArrayList<>());
    }
}
