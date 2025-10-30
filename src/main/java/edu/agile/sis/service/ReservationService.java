package edu.agile.sis.service;

import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.dao.ReservationDAO;
import edu.agile.sis.security.AuthSession;
import org.bson.Document;

import java.util.Date;
import java.util.List;

public class ReservationService {
    private final ReservationDAO reservationDAO = new ReservationDAO();

    /**
     * Create a reservation.
     * - Admin creations are confirmed by default.
     * - Staff creations are created with status = "pending".
     * Returns created reservation id (hex) or null on conflict/failure.
     */
    public String createReservation(String roomId, Date start, Date end, String createdBy, String purpose){
        // allow Admin/Professor/TA/Staff to create bookings
        boolean allowed = AuthSession.getInstance().hasRole("Admin")
                || AuthSession.getInstance().hasRole("Professor")
                || AuthSession.getInstance().hasRole("TA")
                || AuthSession.getInstance().hasRole("Staff")
                || AuthSession.getInstance().hasRole("Lecturer");
        if (!allowed) {
            throw new SecurityException("Insufficient privileges to create reservation");
        }

        // Conflict check: admins can create even if overlap? (we keep conflict check for all)
        List<Document> conflicts = reservationDAO.findOverlapping(roomId, start, end);
        if (!conflicts.isEmpty()) return null; // conflict exists

        String status = AuthSession.getInstance().hasRole("Admin") ? "confirmed" : "pending";

        Document doc = new Document("roomId", roomId)
                .append("start", start)
                .append("end", end)
                .append("createdBy", createdBy)
                .append("purpose", purpose)
                .append("status", status)
                .append("createdAt", new Date())
                .append("updatedAt", new Date());
        reservationDAO.insertReservation(doc);
        Object idObj = doc.get("_id");
        if (idObj instanceof org.bson.types.ObjectId) {
            return ((org.bson.types.ObjectId) idObj).toHexString();
        } else if (idObj != null) {
            return idObj.toString();
        } else {
            return null;
        }
    }

    /**
     * Update an existing reservation by id. Only Admin can update.
     * Returns true on success, false on conflict or failure.
     */
    public boolean updateReservation(String reservationId, Document updatedData){
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin may update reservations");
        }
        // conflict check (excluding current)
        String roomId = updatedData.getString("roomId");
        Date start = updatedData.getDate("start");
        Date end = updatedData.getDate("end");
        if (roomId != null && start != null && end != null) {
            List<Document> conflicts = reservationDAO.findOverlappingExcluding(roomId, start, end, reservationId);
            if (!conflicts.isEmpty()) return false; // conflict exists
        }
        UpdateResult res = reservationDAO.updateReservation(reservationId, updatedData);
        return res.getModifiedCount() > 0;
    }

    public List<Document> listAllReservations(){ return reservationDAO.findAllReservations(); }

    public List<Document> getReservationsByRoom(String roomId){ return reservationDAO.findByRoomId(roomId); }

    public void deleteReservation(String reservationId){
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin may delete reservations");
        }
        reservationDAO.deleteReservation(reservationId);
    }

    public Document getReservationById(String id) { return reservationDAO.findById(id); }

    /**
     * Admin-only: Approve a pending reservation -> set status = "confirmed"
     * Returns true if updated.
     */
    public boolean approveReservation(String reservationId) {
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin may approve reservations");
        }
        Document upd = new Document("status", "confirmed").append("updatedAt", new Date());
        UpdateResult res = reservationDAO.updateReservation(reservationId, new Document("$set", upd));
        return res.getModifiedCount() > 0;
    }

    /**
     * Admin-only: Reject a pending reservation -> set status = "rejected"
     * Returns true if updated.
     */
    public boolean rejectReservation(String reservationId) {
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin may reject reservations");
        }
        Document upd = new Document("status", "rejected").append("updatedAt", new Date());
        UpdateResult res = reservationDAO.updateReservation(reservationId, new Document("$set", upd));
        return res.getModifiedCount() > 0;
    }
}
