package edu.agile.sis.model;

import java.util.Date;

public class Reservation {
    private String roomId;
    private Date start;
    private Date end;
    private String createdBy;
    private String purpose;
    private String status;

    public Reservation(String roomId, Date start, Date end, String createdBy, String purpose){
        this.roomId = roomId;
        this.start = start;
        this.end = end;
        this.createdBy = createdBy;
        this.purpose = purpose;
        this.status = "confirmed";
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public Date getStart() { return start; }
    public void setStart(Date start) { this.start = start; }
    public Date getEnd() { return end; }
    public void setEnd(Date end) { this.end = end; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
