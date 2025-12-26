package edu.agile.sis.service;

import edu.agile.sis.dao.PublicationDAO;
import edu.agile.sis.security.AuthSession;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Service class for managing research publications.
 * Professors can add, edit, delete, and publish their research papers.
 */
public class PublicationService {
    private final PublicationDAO publicationDAO = new PublicationDAO();

    /**
     * Create a new publication. Only professors/lecturers can create.
     */
    public String createPublication(String title, String publicationType, String venue,
            Date publicationDate, String abstractText, String doi,
            String url, List<String> keywords, List<String> coAuthors) {
        checkProfessorAccess("create publications");

        Document doc = new Document()
                .append("authorId", getCurrentUserId())
                .append("authorName", AuthSession.getInstance().getUsername())
                .append("title", title)
                .append("publicationType", publicationType)
                .append("venue", venue)
                .append("publicationDate", publicationDate)
                .append("abstractText", abstractText)
                .append("doi", doi)
                .append("url", url)
                .append("keywords", keywords != null ? keywords : new ArrayList<>())
                .append("coAuthors", coAuthors != null ? coAuthors : new ArrayList<>())
                .append("published", false)
                .append("createdAt", new Date())
                .append("updatedAt", new Date());

        ObjectId id = publicationDAO.insertPublication(doc);
        return id != null ? id.toHexString() : null;
    }

    /**
     * Update an existing publication. Only the author can update.
     */
    public boolean updatePublication(String publicationId, String title, String publicationType,
            String venue, Date publicationDate, String abstractText,
            String doi, String url, List<String> keywords, List<String> coAuthors) {
        checkProfessorAccess("update publications");

        Document existing = publicationDAO.findById(publicationId);
        if (existing == null) {
            throw new IllegalArgumentException("Publication not found: " + publicationId);
        }

        // Verify ownership
        if (!getCurrentUserId().equals(existing.getString("authorId"))) {
            throw new SecurityException("You can only edit your own publications");
        }

        Document update = new Document()
                .append("title", title)
                .append("publicationType", publicationType)
                .append("venue", venue)
                .append("publicationDate", publicationDate)
                .append("abstractText", abstractText)
                .append("doi", doi)
                .append("url", url)
                .append("keywords", keywords != null ? keywords : new ArrayList<>())
                .append("coAuthors", coAuthors != null ? coAuthors : new ArrayList<>())
                .append("updatedAt", new Date());

        return publicationDAO.updatePublication(publicationId, update).getModifiedCount() > 0;
    }

    /**
     * Toggle publication visibility. Only the author can publish/unpublish.
     */
    public boolean togglePublished(String publicationId) {
        checkProfessorAccess("publish/unpublish publications");

        Document existing = publicationDAO.findById(publicationId);
        if (existing == null) {
            throw new IllegalArgumentException("Publication not found: " + publicationId);
        }

        // Verify ownership
        if (!getCurrentUserId().equals(existing.getString("authorId"))) {
            throw new SecurityException("You can only publish/unpublish your own publications");
        }

        boolean currentStatus = existing.getBoolean("published", false);
        Document update = new Document()
                .append("published", !currentStatus)
                .append("updatedAt", new Date());

        return publicationDAO.updatePublication(publicationId, update).getModifiedCount() > 0;
    }

    /**
     * Delete a publication. Only the author can delete.
     */
    public void deletePublication(String publicationId) {
        checkProfessorAccess("delete publications");

        Document existing = publicationDAO.findById(publicationId);
        if (existing == null) {
            throw new IllegalArgumentException("Publication not found: " + publicationId);
        }

        // Verify ownership
        if (!getCurrentUserId().equals(existing.getString("authorId"))) {
            throw new SecurityException("You can only delete your own publications");
        }

        publicationDAO.deletePublication(publicationId);
    }

    /**
     * Get all publications by the current user.
     */
    public List<Document> getMyPublications() {
        checkProfessorAccess("view own publications");
        return publicationDAO.findByAuthor(getCurrentUserId());
    }

    /**
     * Get a publication by ID.
     */
    public Document getPublicationById(String publicationId) {
        return publicationDAO.findById(publicationId);
    }

    /**
     * Get all published publications (public view).
     */
    public List<Document> getAllPublishedPublications() {
        return publicationDAO.findPublished();
    }

    /**
     * Get published publications by a specific author (public view).
     */
    public List<Document> getPublishedByAuthor(String authorId) {
        return publicationDAO.findPublishedByAuthor(authorId);
    }

    /**
     * Search publications by keyword.
     */
    public List<Document> searchPublications(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPublishedPublications();
        }
        return publicationDAO.search(query.trim());
    }

    /**
     * Get publications by type.
     */
    public List<Document> getByType(String publicationType) {
        return publicationDAO.findByType(publicationType);
    }

    // ========== Helper Methods ==========

    private void checkProfessorAccess(String action) {
        AuthSession session = AuthSession.getInstance();
        if (!session.hasRole("Professor") && !session.hasRole("Lecturer") && !session.hasRole("Admin")) {
            throw new SecurityException("Only Professors/Lecturers can " + action);
        }
    }

    private String getCurrentUserId() {
        String linkedId = AuthSession.getInstance().getLinkedEntityId();
        if (linkedId != null && !linkedId.isEmpty()) {
            return linkedId;
        }
        return AuthSession.getInstance().getUsername();
    }
}
