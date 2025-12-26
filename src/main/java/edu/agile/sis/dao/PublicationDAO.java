package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Data Access Object for publications collection.
 */
public class PublicationDAO {
    private final MongoCollection<Document> publications;

    public PublicationDAO() {
        this.publications = DBConnection.getInstance().getDatabase().getCollection("publications");
    }

    public ObjectId insertPublication(Document publication) {
        publications.insertOne(publication);
        return publication.getObjectId("_id");
    }

    public List<Document> findAll() {
        return publications.find()
                .sort(Sorts.descending("publicationDate"))
                .into(new ArrayList<>());
    }

    public List<Document> findPublished() {
        return publications.find(Filters.eq("published", true))
                .sort(Sorts.descending("publicationDate"))
                .into(new ArrayList<>());
    }

    public Document findById(String publicationId) {
        ObjectId oid = tryParseObjectId(publicationId);
        if (oid == null) {
            return publications.find(Filters.eq("_id", publicationId)).first();
        }
        return publications.find(Filters.eq("_id", oid)).first();
    }

    public List<Document> findByAuthor(String authorId) {
        return publications.find(Filters.eq("authorId", authorId))
                .sort(Sorts.descending("publicationDate"))
                .into(new ArrayList<>());
    }

    public List<Document> findPublishedByAuthor(String authorId) {
        return publications.find(Filters.and(
                Filters.eq("authorId", authorId),
                Filters.eq("published", true)))
                .sort(Sorts.descending("publicationDate"))
                .into(new ArrayList<>());
    }

    /**
     * Search publications by keyword in title, abstract, or keywords list.
     */
    public List<Document> search(String searchQuery) {
        Pattern pattern = Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE);
        return publications.find(Filters.and(
                Filters.eq("published", true),
                Filters.or(
                        Filters.regex("title", pattern),
                        Filters.regex("abstractText", pattern),
                        Filters.regex("authorName", pattern),
                        Filters.in("keywords", searchQuery.toLowerCase()))))
                .sort(Sorts.descending("publicationDate"))
                .into(new ArrayList<>());
    }

    public List<Document> findByType(String publicationType) {
        return publications.find(Filters.and(
                Filters.eq("publicationType", publicationType),
                Filters.eq("published", true)))
                .sort(Sorts.descending("publicationDate"))
                .into(new ArrayList<>());
    }

    public UpdateResult updatePublication(String publicationId, Document updatedData) {
        ObjectId oid = tryParseObjectId(publicationId);
        if (oid == null) {
            return publications.updateOne(Filters.eq("_id", publicationId), new Document("$set", updatedData));
        }
        return publications.updateOne(Filters.eq("_id", oid), new Document("$set", updatedData));
    }

    public void deletePublication(String publicationId) {
        ObjectId oid = tryParseObjectId(publicationId);
        if (oid == null) {
            publications.deleteOne(Filters.eq("_id", publicationId));
        } else {
            publications.deleteOne(Filters.eq("_id", oid));
        }
    }

    private ObjectId tryParseObjectId(String id) {
        if (id == null)
            return null;
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
