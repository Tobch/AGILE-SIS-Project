package edu.agile.sis.service;

import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.dao.AuditLogDAO;
import edu.agile.sis.dao.InventoryDAO;
import edu.agile.sis.security.AuthSession;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service class for inventory/resource management.
 * Handles business logic and role-based access control.
 * 
 * - Laptops/Equipment: Single user assignment
 * - Licenses: Multi-user assignment support
 */
public class InventoryService {
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final EntityService entityService = new EntityService("entities");

    /**
     * Create a new inventory item. Only Admin and Staff can create items.
     * 
     * @param name     Item name
     * @param itemType Type of item (Laptop, License, Equipment)
     * @param notes    Optional notes
     * @return The created item ID or null on failure
     */
    public String createItem(String name, String itemType, String notes) {
        checkAdminOrStaffAccess("create inventory items");

        Document doc = new Document()
                .append("name", name)
                .append("itemType", itemType)
                .append("status", "Available")
                .append("notes", notes)
                .append("purchaseDate", new Date())
                .append("createdAt", new Date())
                .append("createdBy", getCurrentUserId());

        // For licenses, initialize empty assignedUsers array
        if ("License".equals(itemType)) {
            doc.append("assignedUsers", new ArrayList<Document>());
        }

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
     * Validate that a user exists and the name matches.
     * 
     * @param userId   The entity ID to look up (e.g., "STU-001", "PROF-001")
     * @param userName The expected name of the user
     * @throws IllegalArgumentException if user not found or name doesn't match
     */
    public void validateUser(String userId, String userName) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("User Name is required");
        }

        Document entity = entityService.getEntityById(userId);
        if (entity == null) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        // Get the name from the entity's core document
        Document core = entity.get("core", Document.class);
        if (core == null) {
            throw new IllegalArgumentException("User data is invalid for ID: " + userId);
        }

        // Check both "name" and "fullName" fields
        String entityName = core.getString("name");
        if (entityName == null) {
            entityName = core.getString("fullName");
        }
        if (entityName == null) {
            // Try firstName + lastName
            String firstName = core.getString("firstName");
            String lastName = core.getString("lastName");
            if (firstName != null && lastName != null) {
                entityName = firstName + " " + lastName;
            }
        }

        if (entityName == null) {
            throw new IllegalArgumentException("Could not determine user name for ID: " + userId);
        }

        // Case-insensitive comparison, trimmed
        if (!entityName.trim().equalsIgnoreCase(userName.trim())) {
            throw new IllegalArgumentException(
                    "Name mismatch: Expected '" + entityName + "' but got '" + userName + "' for ID: " + userId);
        }
    }

    /**
     * Allocate (assign) a Laptop or Equipment to a user.
     * For Licenses, use addUserToLicense() instead.
     * 
     * @param itemId   The inventory item ID
     * @param userId   The user ID to assign to (entity ID)
     * @param userName The display name of the user
     * @return true if successful
     */
    public boolean allocateItem(String itemId, String userId, String userName) {
        checkAdminOrStaffAccess("allocate inventory items");

        // Validate user exists and name matches
        validateUser(userId, userName);

        Document item = inventoryDAO.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        String itemType = item.getString("itemType");

        // For licenses, redirect to addUserToLicense
        if ("License".equals(itemType)) {
            return addUserToLicense(itemId, userId, userName);
        }

        // For Laptop/Equipment: single user assignment
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
     * Add a user to a License (supports multiple users).
     * 
     * @param itemId   The license item ID
     * @param userId   The user ID to add
     * @param userName The display name of the user
     * @return true if successful
     */
    public boolean addUserToLicense(String itemId, String userId, String userName) {
        checkAdminOrStaffAccess("allocate license to users");

        // Validate user exists and name matches
        validateUser(userId, userName);

        Document item = inventoryDAO.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        if (!"License".equals(item.getString("itemType"))) {
            throw new IllegalArgumentException(
                    "This method is only for License items. Use allocateItem for Laptop/Equipment.");
        }

        // Get current assigned users
        @SuppressWarnings("unchecked")
        List<Document> assignedUsers = item.get("assignedUsers", List.class);
        if (assignedUsers == null) {
            assignedUsers = new ArrayList<>();
        }

        // Check if user is already assigned
        for (Document user : assignedUsers) {
            if (userId.equals(user.getString("userId"))) {
                throw new IllegalStateException("User " + userName + " is already assigned to this license.");
            }
        }

        // Add new user
        Document newUser = new Document()
                .append("userId", userId)
                .append("userName", userName)
                .append("assignedDate", new Date());
        assignedUsers.add(newUser);

        Document update = new Document()
                .append("status", "Assigned")
                .append("assignedUsers", assignedUsers)
                .append("updatedAt", new Date());

        UpdateResult result = inventoryDAO.updateItem(itemId, update);

        if (result.getModifiedCount() > 0) {
            logAuditEntry(itemId, "ALLOCATE_LICENSE",
                    "Added user to license: " + userName + " (ID: " + userId + "). Total users: "
                            + assignedUsers.size());
            return true;
        }
        return false;
    }

    /**
     * Remove a specific user from a License.
     * 
     * @param itemId The license item ID
     * @param userId The user ID to remove
     * @return true if successful
     */
    public boolean removeUserFromLicense(String itemId, String userId) {
        checkAdminOrStaffAccess("deallocate license from users");

        Document item = inventoryDAO.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        if (!"License".equals(item.getString("itemType"))) {
            throw new IllegalArgumentException("This method is only for License items.");
        }

        @SuppressWarnings("unchecked")
        List<Document> assignedUsers = item.get("assignedUsers", List.class);
        if (assignedUsers == null || assignedUsers.isEmpty()) {
            throw new IllegalStateException("No users are assigned to this license.");
        }

        String removedUserName = null;
        List<Document> updatedUsers = new ArrayList<>();
        for (Document user : assignedUsers) {
            if (userId.equals(user.getString("userId"))) {
                removedUserName = user.getString("userName");
            } else {
                updatedUsers.add(user);
            }
        }

        if (removedUserName == null) {
            throw new IllegalArgumentException("User with ID " + userId + " is not assigned to this license.");
        }

        String newStatus = updatedUsers.isEmpty() ? "Available" : "Assigned";

        Document update = new Document()
                .append("status", newStatus)
                .append("assignedUsers", updatedUsers)
                .append("updatedAt", new Date());

        UpdateResult result = inventoryDAO.updateItem(itemId, update);

        if (result.getModifiedCount() > 0) {
            logAuditEntry(itemId, "DEALLOCATE_LICENSE",
                    "Removed user from license: " + removedUserName + " (ID: " + userId + "). Remaining users: "
                            + updatedUsers.size());
            return true;
        }
        return false;
    }

    /**
     * Deallocate (unassign) a Laptop or Equipment from its current user.
     * For Licenses, use removeUserFromLicense() instead.
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

        String itemType = item.getString("itemType");

        // For licenses, clear all users
        if ("License".equals(itemType)) {
            @SuppressWarnings("unchecked")
            List<Document> assignedUsers = item.get("assignedUsers", List.class);
            int userCount = assignedUsers != null ? assignedUsers.size() : 0;

            Document update = new Document()
                    .append("status", "Available")
                    .append("assignedUsers", new ArrayList<Document>())
                    .append("updatedAt", new Date());

            UpdateResult result = inventoryDAO.updateItem(itemId, update);
            if (result.getModifiedCount() > 0) {
                logAuditEntry(itemId, "DEALLOCATE", "Removed all " + userCount + " users from license");
                return true;
            }
            return false;
        }

        // For Laptop/Equipment
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

    /**
     * Get licenses where the given user is assigned.
     */
    public List<Document> getLicensesByUser(String userId) {
        return inventoryDAO.findByUserInAssignedUsers(userId);
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

    // ========== REQUEST/APPROVAL METHODS ==========

    private final edu.agile.sis.dao.InventoryRequestDAO requestDAO = new edu.agile.sis.dao.InventoryRequestDAO();

    /**
     * Get available items that the current user can request.
     * Students: Laptops and Licenses only
     * Staff/Professors: All types
     * Note: Licenses are always shown (even if assigned) since they support
     * multiple users.
     */
    public List<Document> getAvailableItemsForRequest() {
        // Get all items, not just "Available" ones
        List<Document> allItems = inventoryDAO.findAllItems();
        List<String> currentUserRoles = AuthSession.getInstance().getRoles();
        String currentUserId = getCurrentUserId();

        boolean isStudent = currentUserRoles.contains("Student");

        List<Document> result = new ArrayList<>();
        for (Document item : allItems) {
            String type = item.getString("itemType");
            String status = item.getString("status");

            // Skip items under repair
            if ("Under Repair".equals(status)) {
                continue;
            }

            // For Laptop/Equipment: only show if Available
            if ("Laptop".equals(type) || "Equipment".equals(type)) {
                if (!"Available".equals(status)) {
                    continue;
                }
                // Students can't request Equipment
                if (isStudent && "Equipment".equals(type)) {
                    continue;
                }
            }

            // For License: show even if Assigned (supports multiple users)
            // But check if this user is already assigned
            if ("License".equals(type)) {
                @SuppressWarnings("unchecked")
                List<Document> assignedUsers = item.get("assignedUsers", List.class);
                if (assignedUsers != null) {
                    boolean alreadyAssigned = false;
                    for (Document user : assignedUsers) {
                        if (currentUserId.equals(user.getString("userId"))) {
                            alreadyAssigned = true;
                            break;
                        }
                    }
                    if (alreadyAssigned) {
                        continue; // User already has this license
                    }
                }
            }

            // Students can only see Laptops and Licenses
            if (isStudent && !"Laptop".equals(type) && !"License".equals(type)) {
                continue;
            }

            result.add(item);
        }

        return result;
    }

    /**
     * Submit a request for an inventory item.
     * 
     * @param itemId The item to request
     * @param notes  Reason/notes for the request
     * @return The request ID or null on failure
     */
    public String submitRequest(String itemId, String notes) {
        Document item = inventoryDAO.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        String itemType = item.getString("itemType");
        List<String> roles = AuthSession.getInstance().getRoles();
        boolean isStudent = roles.contains("Student");

        // Check if student is requesting Equipment (not allowed)
        if (isStudent && "Equipment".equals(itemType)) {
            throw new SecurityException("Students cannot request Equipment items.");
        }

        // For Laptop/Equipment, check if already assigned
        if (!"License".equals(itemType) && "Assigned".equals(item.getString("status"))) {
            throw new IllegalStateException("This item is already assigned to someone.");
        }

        String requesterId = getCurrentUserId();
        String requesterName = AuthSession.getInstance().getUsername();

        // Check if user already has a pending request for this item
        Document existingRequest = requestDAO.findPendingByRequesterAndItem(requesterId, itemId);
        if (existingRequest != null) {
            throw new IllegalStateException("You already have a pending request for this item.");
        }

        // Determine requester type
        String requesterType = "User";
        if (roles.contains("Student"))
            requesterType = "Student";
        else if (roles.contains("Professor"))
            requesterType = "Professor";
        else if (roles.contains("Staff"))
            requesterType = "Staff";
        else if (roles.contains("Lecturer"))
            requesterType = "Lecturer";
        else if (roles.contains("TA"))
            requesterType = "TA";

        Document request = new Document()
                .append("itemId", itemId)
                .append("itemName", item.getString("name"))
                .append("itemType", itemType)
                .append("requesterId", requesterId)
                .append("requesterName", requesterName)
                .append("requesterType", requesterType)
                .append("requestDate", new Date())
                .append("status", "Pending")
                .append("notes", notes);

        org.bson.types.ObjectId id = requestDAO.insertRequest(request);

        logAuditEntry(itemId, "REQUEST_SUBMITTED",
                "Request submitted by " + requesterName + " for item: " + item.getString("name"));

        return id != null ? id.toHexString() : null;
    }

    /**
     * Get the current user's requests.
     */
    public List<Document> getMyRequests() {
        return requestDAO.findByRequester(getCurrentUserId());
    }

    /**
     * Get all pending requests (Admin only).
     */
    public List<Document> getPendingRequests() {
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin can view all pending requests");
        }
        return requestDAO.findByStatus("Pending");
    }

    /**
     * Get all requests (Admin only).
     */
    public List<Document> getAllRequests() {
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin can view all requests");
        }
        return requestDAO.findAllRequests();
    }

    /**
     * Approve a request and assign the item to the requester.
     * 
     * @param requestId   The request to approve
     * @param reviewNotes Admin's notes
     * @return true if successful
     */
    public boolean approveRequest(String requestId, String reviewNotes) {
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin can approve requests");
        }

        Document request = requestDAO.findById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request not found: " + requestId);
        }

        if (!"Pending".equals(request.getString("status"))) {
            throw new IllegalStateException("This request has already been processed.");
        }

        String itemId = request.getString("itemId");
        String requesterId = request.getString("requesterId");
        String requesterName = request.getString("requesterName");
        String itemType = request.getString("itemType");

        // Update request status
        Document requestUpdate = new Document()
                .append("status", "Approved")
                .append("reviewedBy", getCurrentUserId())
                .append("reviewerName", AuthSession.getInstance().getUsername())
                .append("reviewDate", new Date())
                .append("reviewNotes", reviewNotes);

        requestDAO.updateRequest(requestId, requestUpdate);

        // Assign the item to the requester
        if ("License".equals(itemType)) {
            // For licenses, add user to the list
            addUserToLicenseInternal(itemId, requesterId, requesterName);
        } else {
            // For Laptop/Equipment, single assignment
            Document itemUpdate = new Document()
                    .append("status", "Assigned")
                    .append("assignedToUserId", requesterId)
                    .append("assignedToName", requesterName)
                    .append("assignedDate", new Date())
                    .append("updatedAt", new Date());
            inventoryDAO.updateItem(itemId, itemUpdate);
        }

        logAuditEntry(itemId, "REQUEST_APPROVED",
                "Request approved for " + requesterName + " by " + AuthSession.getInstance().getUsername());

        return true;
    }

    /**
     * Internal method to add user to license without re-validation.
     */
    private void addUserToLicenseInternal(String itemId, String userId, String userName) {
        Document item = inventoryDAO.findById(itemId);
        if (item == null)
            return;

        @SuppressWarnings("unchecked")
        List<Document> assignedUsers = item.get("assignedUsers", List.class);
        if (assignedUsers == null) {
            assignedUsers = new ArrayList<>();
        }

        // Check if already assigned
        for (Document user : assignedUsers) {
            if (userId.equals(user.getString("userId"))) {
                return; // Already assigned
            }
        }

        Document newUser = new Document()
                .append("userId", userId)
                .append("userName", userName)
                .append("assignedDate", new Date());
        assignedUsers.add(newUser);

        Document update = new Document()
                .append("status", "Assigned")
                .append("assignedUsers", assignedUsers)
                .append("updatedAt", new Date());

        inventoryDAO.updateItem(itemId, update);
    }

    /**
     * Reject a request.
     * 
     * @param requestId   The request to reject
     * @param reviewNotes Reason for rejection
     * @return true if successful
     */
    public boolean rejectRequest(String requestId, String reviewNotes) {
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin can reject requests");
        }

        Document request = requestDAO.findById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request not found: " + requestId);
        }

        if (!"Pending".equals(request.getString("status"))) {
            throw new IllegalStateException("This request has already been processed.");
        }

        Document requestUpdate = new Document()
                .append("status", "Rejected")
                .append("reviewedBy", getCurrentUserId())
                .append("reviewerName", AuthSession.getInstance().getUsername())
                .append("reviewDate", new Date())
                .append("reviewNotes", reviewNotes);

        requestDAO.updateRequest(requestId, requestUpdate);

        String itemId = request.getString("itemId");
        String requesterName = request.getString("requesterName");

        logAuditEntry(itemId, "REQUEST_REJECTED",
                "Request rejected for " + requesterName + ". Reason: " + reviewNotes);

        return true;
    }

    /**
     * Cancel own pending request.
     */
    public boolean cancelRequest(String requestId) {
        Document request = requestDAO.findById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Request not found: " + requestId);
        }

        // Verify ownership
        if (!getCurrentUserId().equals(request.getString("requesterId"))) {
            throw new SecurityException("You can only cancel your own requests");
        }

        if (!"Pending".equals(request.getString("status"))) {
            throw new IllegalStateException("Only pending requests can be cancelled.");
        }

        requestDAO.deleteRequest(requestId);

        logAuditEntry(request.getString("itemId"), "REQUEST_CANCELLED",
                "Request cancelled by " + AuthSession.getInstance().getUsername());

        return true;
    }

    /**
     * Get items assigned to the current user.
     */
    public List<Document> getMyAssignedItems() {
        String userId = getCurrentUserId();
        List<Document> result = new ArrayList<>();

        // Get directly assigned items (Laptop/Equipment)
        result.addAll(inventoryDAO.findByAssignedUser(userId));

        // Get licenses where user is in assignedUsers
        result.addAll(inventoryDAO.findByUserInAssignedUsers(userId));

        return result;
    }
}
