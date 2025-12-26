package edu.agile.sis.service;

import edu.agile.sis.dao.EventDAO;
import edu.agile.sis.dao.RSVPDAO;
import org.bson.Document;

import java.util.List;

/**
 * RSVP business logic
 */
public class RSVPService {

    private final RSVPDAO rsvpDAO = new RSVPDAO();
    private final EventDAO eventDAO = new EventDAO();

    /**
     * RSVP a student to an event
     */
    public boolean rsvp(String eventId, String studentId, String username) {

        if (eventId == null || studentId == null)
            throw new IllegalArgumentException("eventId and studentId required");

        // Prevent duplicate RSVP
        List<Document> existing = rsvpDAO.listByEvent(eventId);
        boolean alreadyJoined = existing.stream()
                .anyMatch(d -> studentId.equals(d.getString("studentId")));
        if (alreadyJoined) return false;

        // Capacity check
        Document event = eventDAO.findById(eventId);
        if (event == null)
            throw new IllegalArgumentException("Event not found");

        Integer capacity = event.containsKey("capacity")
                ? event.getInteger("capacity")
                : null;

        if (capacity != null && existing.size() >= capacity)
            return false;

        // Insert RSVP
        return rsvpDAO.insert(eventId, studentId, username) != null;
    }

    /**
     * List attendees
     */
    public List<Document> listAttendees(String eventId) {
        return rsvpDAO.listByEvent(eventId);
    }

    /**
     * Check if a student already RSVP'd
     */
    public boolean isAttending(String eventId, String studentId) {
        return rsvpDAO.listByEvent(eventId).stream()
                .anyMatch(d -> studentId.equals(d.getString("studentId")));
    }

    /**
     * Count attendees
     */
    public int countAttendees(String eventId) {
        return rsvpDAO.listByEvent(eventId).size();
    }
}
