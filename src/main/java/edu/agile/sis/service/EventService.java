package edu.agile.sis.service;

import edu.agile.sis.dao.EventDAO;
import org.bson.Document;

import java.util.Date;
import java.util.List;

/**
 * Service layer for Events (read + create only for Sprint 1)
 */
public class EventService {

    private final EventDAO eventDAO = new EventDAO();

    /**
     * Create a new event
     */
    public String createEvent(Document doc) {
        if (doc == null)
            throw new IllegalArgumentException("Event document cannot be null");

        if (!doc.containsKey("title") || doc.getString("title").isBlank())
            throw new IllegalArgumentException("Event title is required");

        doc.putIfAbsent("createdAt", new Date());
        doc.putIfAbsent("deleted", false);

        return eventDAO.insert(doc);
    }

    /**
     * List upcoming events (optionally filtered by category)
     */
    public List<Document> listUpcoming(Date from, int skip, int limit, String category) {
        return eventDAO.listUpcoming(from, skip, limit, category);
    }

    /**
     * Get event by ID
     */
    public Document getEventById(String id) {
        return eventDAO.findById(id);
    }
}
