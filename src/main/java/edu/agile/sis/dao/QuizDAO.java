package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO for quizzes collection.
 *
 * Quiz document example:
 * {
 *   "_id": ObjectId(...),
 *   "courseCode": "CSE231",
 *   "title": "Quiz 1",
 *   "timeLimitMinutes": 5,
 *   "questions": [ ... ],
 *   "createdBy": "Dr.Gamal",
 *   "createdAt": ISODate(...)
 * }
 */
public class QuizDAO {
    private final MongoCollection<Document> coll;

    public QuizDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("quizzes");
    }

    /**
     * Insert a new quiz document. Returns the inserted id as hex string (ObjectId) or plain string id.
     */
    public String insert(Document quiz) {
        coll.insertOne(quiz);
        Object id = quiz.get("_id");
        if (id instanceof ObjectId) return ((ObjectId) id).toHexString();
        return id == null ? null : id.toString();
    }

    /**
     * List quizzes by courseCode. If courseCode is null or blank, return all quizzes.
     */
    public List<Document> listByCourse(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return coll.find().sort(Sorts.ascending("createdAt")).into(new ArrayList<>());
        }
        return coll.find(Filters.eq("courseCode", courseCode)).sort(Sorts.ascending("createdAt")).into(new ArrayList<>());
    }

    /**
     * Find a quiz by id. Accepts hex string for ObjectId or string id.
     */
    public Document findById(String hexId) {
        if (hexId == null) return null;
        try {
            return coll.find(Filters.eq("_id", new ObjectId(hexId))).first();
        } catch (IllegalArgumentException ex) {
            return coll.find(Filters.eq("_id", hexId)).first();
        }
    }

    /**
     * Update a quiz by id (hex string). Returns true when update attempted.
     */
    public boolean update(String hexId, Document updates) {
        if (hexId == null) return false;
        try {
            coll.updateOne(Filters.eq("_id", new ObjectId(hexId)), new Document("$set", updates));
            return true;
        } catch (Exception ex) {
            try {
                coll.updateOne(Filters.eq("_id", hexId), new Document("$set", updates));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Delete a quiz by id (hex string). Returns true on success.
     */
    public boolean delete(String hexId) {
        if (hexId == null) return false;
        try {
            coll.deleteOne(Filters.eq("_id", new ObjectId(hexId)));
            return true;
        } catch (IllegalArgumentException ex) {
            coll.deleteOne(Filters.eq("_id", hexId));
            return true;
        }
    }
}
