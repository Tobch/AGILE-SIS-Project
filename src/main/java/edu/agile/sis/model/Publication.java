package edu.agile.sis.model;

import java.util.Date;
import java.util.List;

/**
 * Model class representing a research publication.
 * Professors can add their research papers, journal articles, etc.
 */
public class Publication {
    private String publicationId;
    private String authorId; // Professor's entity ID
    private String authorName; // Display name
    private String title;
    private String abstractText; // 'abstract' is a reserved keyword
    private String publicationType; // "Journal Article", "Conference Paper", "Book Chapter", "Thesis"
    private String venue; // Journal/Conference name
    private Date publicationDate;
    private String doi; // Digital Object Identifier
    private String url; // External link to paper
    private List<String> keywords;
    private List<String> coAuthors; // Names of co-authors
    private boolean published; // Whether visible to public
    private Date createdAt;
    private Date updatedAt;

    public Publication() {
        this.createdAt = new Date();
        this.published = false;
    }

    public Publication(String authorId, String authorName, String title, String publicationType) {
        this();
        this.authorId = authorId;
        this.authorName = authorName;
        this.title = title;
        this.publicationType = publicationType;
    }

    // Getters and Setters
    public String getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(String publicationId) {
        this.publicationId = publicationId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getPublicationType() {
        return publicationType;
    }

    public void setPublicationType(String publicationType) {
        this.publicationType = publicationType;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getCoAuthors() {
        return coAuthors;
    }

    public void setCoAuthors(List<String> coAuthors) {
        this.coAuthors = coAuthors;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
