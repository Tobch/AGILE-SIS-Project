package edu.agile.sis.service;

import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.dao.AuditLogDAO;
import edu.agile.sis.dao.InventoryDAO;
import edu.agile.sis.security.AuthSession;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

/**
 * Service class for inventory/resource management.
 * Handles business logic and role-based access control.
 */
public class InventoryService {
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /**
     * Create a new inventory item. Only Admin and Staff can create items.
     * 
     * @return The created item ID or null on failure
     */
    public String createItem(String name, String itemType, String departmentId, String notes) {
        checkAdminOrStaffAccess("create inventory items");

        Document doc = new Document()
                .append("name", name)
                .append("itemType", itemType)
                .append("status", "Available")
                .append("departmentId", departmentId)
                .append("notes", notes)
                .append("purchaseDate", new Date())
                .append("createdAt", new Date())
                .append("createdBy", getCurrentUserId());

        ObjectId id = inventoryDAO.insertItem(doc);

        // Log the creation
        logAuditEntry(id.toHexString(), "CREATE",
                "Created inventory item: " + name);

        return id != null ? id.toHexString() : null;
    }

    /**
     * Update an existing inventory item. Only Admin and Staff can update.
     */
    public boolean updateItem(String itemId, Document updatedData) {
        checkAdminOrStaffAccess("update inventory items");

        updatedData.append("updatedAt", new Date());
        UpdateResult result = inventoryDAO.updateItem(itemId, updatedData);

        if (result.getModifiedCount() > 0) {
            logAuditEntry(itemId, "UPDATE", "Updated inventory item");
            return true;
        }
        return false;
    }

    /**
     * Delete an inventory item. Only Admin can delete.
     */
    public void deleteItem(String itemId) {
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin may delete inventory items");
        }

        Document item = inventoryDAO.findById(itemId);
        String itemName = item != null ? item.getString("name") : itemId;

        inventoryDAO.deleteItem(itemId);
        logAuditEntry(itemId, "DELETE", "Deleted inventory item: " + itemName);
    }

    /**
     * Allocate (assign) an item to a user.
     * 
     * @param itemId   The inventory item ID
     * @param userId   The user ID to assign to
     * @param userName The display name of the user
     * @return true if successful
     */
    public boolean allocateItem(String itemId, String userId, String userName) {
        checkAdminOrStaffAccess("allocate inventory items");

        Document item = inventoryDAO.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        String currentStatus = item.getString("status");
        if ("Assigned".equals(currentStatus)) {
            throw new IllegalStateException("Item is already assigned to: " + item.getString("assignedToName"));
        }

        Document update = new Document()
                .append("status", "Assigned")
                .append("assignedToUserId", userId)
                .append("assignedToName", userName)
                .append("assignedDate", new Date())
                .append("updatedAt", new Date());

        UpdateResult result = inventoryDAO.updateItem(itemId, update);

        if (result.getModifiedCount() > 0) {
            logAuditEntry(itemId, "ALLOCATE",
                    "Assigned item to: " + userName + " (ID: " + userId + ")");
            return true;
        }
        return false;
    }

    /**
     * Deallocate (unassign) an item from its current user.
     * 
     * @param itemId The inventory item ID
     * @return true if successful
     */
    public boolean deallocateItem(String itemId) {
        checkAdminOrStaffAccess("deallocate inventory items");

        Document item = inventoryDAO.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        String previousUser = item.getString("assignedToName");
        if (previousUser == null || previousUser.isEmpty()) {
            previousUser = "Unknown";
        }

        Document update = new Document()
                .append("status", "Available")
                .append("assignedToUserId", null)
                .append("assignedToName", null)
                .append("assignedDate", null)
                .append("updatedAt", new Date());

        UpdateResult result = inventoryDAO.updateItem(itemId, update);

        if (result.getModifiedCount() > 0) {
            logAuditEntry(itemId, "DEALLOCATE",
                    "Unassigned item from: " + previousUser);
            return true;
        }
        return false;
    }

    /**
     * Set item status to "Under Repair".
     */
    public boolean markUnderRepair(String itemId) {
        checkAdminOrStaffAccess("update inventory status");

        Document update = new Document()
                .append("status", "Under Repair")
                .append("updatedAt", new Date());

        UpdateResult result = inventoryDAO.updateItem(itemId, update);

        if (result.getModifiedCount() > 0) {
            logAuditEntry(itemId, "STATUS_CHANGE", "Marked item as Under Repair");
            return true;
        }
        return false;
    }

    /**
     * Set item status back to "Available" (e.g., after repair).
     */
    public boolean markAvailable(String itemId) {
        checkAdminOrStaffAccess("update inventory status");

        Document update = new Document()
                .append("status", "Available")
                .append("assignedToUserId", null)
                .append("assignedToName", null)
                .append("updatedAt", new Date());

        UpdateResult result = inventoryDAO.updateItem(itemId, update);

        if (result.getModifiedCount() > 0) {
            logAuditEntry(itemId, "STATUS_CHANGE", "Marked item as Available");
            return true;
        }
        return false;
    }

    // === Query Methods ===

    public List<Document> listAllItems() {
        return inventoryDAO.findAllItems();
    }

    public Document getItemById(String itemId) {
        return inventoryDAO.findById(itemId);
    }

    public List<Document> getItemsByStatus(String status) {
        return inventoryDAO.findByStatus(status);
    }

    public List<Document> getItemsByType(String itemType) {
        return inventoryDAO.findByItemType(itemType);
    }

    public List<Document> getItemsByUser(String userId) {
        return inventoryDAO.findByAssignedUser(userId);
    }

    public List<Document> getItemsByDepartment(String departmentId) {
        return inventoryDAO.findByDepartment(departmentId);
    }

    /**
     * Get the audit history for an inventory item.
     */
    public List<Document> getAuditHistory(String itemId) {
        return auditLogDAO.findByEntityId(itemId);
    }

    // === Helper Methods ===

    private void checkAdminOrStaffAccess(String action) {
        boolean allowed = AuthSession.getInstance().hasRole("Admin")
                || AuthSession.getInstance().hasRole("Staff")
                || AuthSession.getInstance().hasRole("Professor")
                || AuthSession.getInstance().hasRole("Lecturer");

        if (!allowed) {
            throw new SecurityException("Insufficient privileges to " + action);
        }
    }

    /**
     * Get the current user's ID. Uses linkedEntityId if available, otherwise
     * username.
     */
    private String getCurrentUserId() {
        String linkedId = AuthSession.getInstance().getLinkedEntityId();
        if (linkedId != null && !linkedId.isEmpty()) {
            return linkedId;
        }
        return AuthSession.getInstance().getUsername();
    }

    private void logAuditEntry(String entityId, String action, String details) {
        Document log = new Document()
                .append("entityType", "inventory")
                .append("entityId", entityId)
                .append("action", action)
                .append("details", details)
                .append("performedBy", getCurrentUserId())
                .append("performedByName", AuthSession.getInstance().getUsername())
                .append("timestamp", new Date());

        auditLogDAO.insertLog(log);
    }
}
