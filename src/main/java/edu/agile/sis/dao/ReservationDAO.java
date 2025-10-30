package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReservationDAO {
    private final MongoCollection<Document> reservations;

    public ReservationDAO() {
        this.reservations = DBConnection.getInstance().getDatabase().getCollection("reservations");
    }

    public ObjectId insertReservation(Document reservation){
        reservations.insertOne(reservation);
        ObjectId id = reservation.getObjectId("_id");
        return id;
    }

    public List<Document> findAllReservations(){
        return reservations.find().into(new ArrayList<>());
    }

    public List<Document> findByRoomId(String roomId){
        return reservations.find(Filters.eq("roomId", roomId)).into(new ArrayList<>());
    }

    public List<Document> findOverlapping(String roomId, Date start, Date end){
        // overlapping where start < existing.end AND end > existing.start
        return reservations.find(Filters.and(
                Filters.eq("roomId", roomId),
                Filters.lt("start", end),
                Filters.gt("end", start)
        )).into(new ArrayList<>());
    }

    /**
     * Find overlapping reservations for a room excluding a specific reservation id.
     * Used when editing an existing reservation.
     */
    public List<Document> findOverlappingExcluding(String roomId, Date start, Date end, String excludeReservationId){
        if (excludeReservationId == null) return findOverlapping(roomId, start, end);
        ObjectId excludeOid = tryParseObjectId(excludeReservationId);
        if (excludeOid == null) {
            // fallback to the normal overlapping query if id is not an ObjectId
            return findOverlapping(roomId, start, end);
        }
        return reservations.find(Filters.and(
                Filters.eq("roomId", roomId),
                Filters.lt("start", end),
                Filters.gt("end", start),
                Filters.ne("_id", excludeOid)
        )).into(new ArrayList<>());
    }

    public UpdateResult updateReservation(String reservationId, Document updatedData){
        ObjectId oid = tryParseObjectId(reservationId);
        if (oid == null) {
            // try to match by string equality (less common)
            return reservations.updateOne(Filters.eq("_id", reservationId), new Document("$set", updatedData));
        } else {
            return reservations.updateOne(Filters.eq("_id", oid), new Document("$set", updatedData));
        }
    }

    public void deleteReservation(String reservationId){
        ObjectId oid = tryParseObjectId(reservationId);
        if (oid == null) {
            reservations.deleteOne(Filters.eq("_id", reservationId));
        } else {
            reservations.deleteOne(Filters.eq("_id", oid));
        }
    }

    public Document findById(String reservationId) {
        ObjectId oid = tryParseObjectId(reservationId);
        if (oid == null) {
            return reservations.find(Filters.eq("_id", reservationId)).first();
        } else {
            return reservations.find(Filters.eq("_id", oid)).first();
        }
    }

    private ObjectId tryParseObjectId(String id){
        if (id == null) return null;
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
