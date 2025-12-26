package edu.agile.sis.model;

import java.util.Date;

/**
 * Model class representing an inventory request.
 * Users submit requests for items, which admins can approve or reject.
 */
public class InventoryRequest {
    private String requestId;
    private String itemId;
    private String itemName;
    private String itemType;
    private String requesterId; // Entity ID of the requester
    private String requesterName; // Display name
    private String requesterType; // "Student", "Staff", "Professor", etc.
    private Date requestDate;
    private String status; // "Pending", "Approved", "Rejected"
    private String notes; // Requester's notes/reason for request
    private String reviewedBy; // Admin who reviewed
    private String reviewerName; // Admin display name
    private Date reviewDate;
    private String reviewNotes; // Admin's notes (e.g., rejection reason)

    public InventoryRequest() {
        this.requestDate = new Date();
        this.status = "Pending";
    }

    public InventoryRequest(String itemId, String itemName, String itemType,
            String requesterId, String requesterName, String requesterType, String notes) {
        this();
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemType = itemType;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterType = requesterType;
        this.notes = notes;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterType() {
        return requesterType;
    }

    public void setRequesterType(String requesterType) {
        this.requesterType = requesterType;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public Date getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(Date reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}
