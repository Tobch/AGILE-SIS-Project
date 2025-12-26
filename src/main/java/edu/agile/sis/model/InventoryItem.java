package edu.agile.sis.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Model class representing an inventory item (laptop, license, equipment).
 * - Laptop/Equipment: Single user assignment (assignedToUserId, assignedToName)
 * - License: Multi-user assignment (assignedUsers list)
 */
public class InventoryItem {
    private String itemId;
    private String name;
    private String itemType; // "Laptop", "License", "Equipment"
    private String status; // "Available", "Assigned", "Under Repair"
    private String assignedToUserId; // For Laptop/Equipment (single user)
    private String assignedToName; // For Laptop/Equipment (single user)
    private List<AssignedUser> assignedUsers; // For License (multi-user)
    private Date purchaseDate;
    private Date assignedDate;
    private String notes;

    /**
     * Inner class to represent an assigned user for licenses.
     */
    public static class AssignedUser {
        private String userId;
        private String userName;
        private Date assignedDate;

        public AssignedUser() {
        }

        public AssignedUser(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
            this.assignedDate = new Date();
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public Date getAssignedDate() {
            return assignedDate;
        }

        public void setAssignedDate(Date assignedDate) {
            this.assignedDate = assignedDate;
        }
    }

    public InventoryItem() {
        this.assignedUsers = new ArrayList<>();
    }

    public InventoryItem(String name, String itemType) {
        this.name = name;
        this.itemType = itemType;
        this.status = "Available";
        this.purchaseDate = new Date();
        this.assignedUsers = new ArrayList<>();
    }

    // Getters and Setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(String assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public List<AssignedUser> getAssignedUsers() {
        return assignedUsers;
    }

    public void setAssignedUsers(List<AssignedUser> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Date getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(Date assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
