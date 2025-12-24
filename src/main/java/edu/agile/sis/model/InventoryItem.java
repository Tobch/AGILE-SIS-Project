package edu.agile.sis.model;

import java.util.Date;

/**
 * Model class representing an inventory item (laptop, license, equipment).
 */
public class InventoryItem {
    private String itemId;
    private String name;
    private String itemType; // "Laptop", "License", "Equipment"
    private String status; // "Available", "Assigned", "Under Repair"
    private String assignedToUserId;
    private String assignedToName;
    private String departmentId;
    private Date purchaseDate;
    private Date assignedDate;
    private String notes;

    public InventoryItem() {
    }

    public InventoryItem(String name, String itemType, String departmentId) {
        this.name = name;
        this.itemType = itemType;
        this.departmentId = departmentId;
        this.status = "Available";
        this.purchaseDate = new Date();
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

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
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
