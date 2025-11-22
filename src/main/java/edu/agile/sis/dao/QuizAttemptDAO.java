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
 * DAO for quiz attempts.
 * Attempt shape:
 * {
 *   "_id": ObjectId,
 *   "quizId": ObjectId or string,
 *   "studentId": "20P1076",
 *   "answers": { "q1":"4", "q2":"b" },
 *   "score": 80.0,
 *   "maxScore": 100.0,
 *   "submittedAt": Date,
 *   "graded": true/false,
 *   "grader": "P1001",
 *   "feedback": "Good"
 * }
 */
public class QuizAttemptDAO {
    private final MongoCollection<Document> coll;

    public QuizAttemptDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("quiz_attempts");
    }

    public String insert(Document attempt) {
        coll.insertOne(attempt);
        Object id = attempt.get("_id");
        if (id instanceof ObjectId) return ((ObjectId) id).toHexString();
        return id == null ? null : id.toString();
    }

    public List<Document> listByQuiz(String quizIdHex) {
        List<Document> result = new ArrayList<>();
        try {
            result = coll.find(Filters.eq("quizId", new ObjectId(quizIdHex))).sort(Sorts.ascending("submittedAt")).into(new ArrayList<>());
        } catch (IllegalArgumentException ex) {
            result = coll.find(Filters.eq("quizId", quizIdHex)).sort(Sorts.ascending("submittedAt")).into(new ArrayList<>());
        }
        return result;
    }

    public List<Document> listByStudent(String studentId) {
        return coll.find(Filters.eq("studentId", studentId)).sort(Sorts.descending("submittedAt")).into(new ArrayList<>());
    }

    public Document findById(String idHex) {
        try {
            return coll.find(Filters.eq("_id", new ObjectId(idHex))).first();
        } catch (IllegalArgumentException ex) {
            return coll.find(Filters.eq("_id", idHex)).first();
        }
    }

    public boolean update(String idHex, Document updates) {
        try {
            coll.updateOne(Filters.eq("_id", new ObjectId(idHex)), new Document("$set", updates));
            return true;
        } catch (Exception ex) {
            try {
                coll.updateOne(Filters.eq("_id", idHex), new Document("$set", updates));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public boolean delete(String idHex) {
        try {
            coll.deleteOne(Filters.eq("_id", new ObjectId(idHex)));
            return true;
        } catch (IllegalArgumentException ex) {
            coll.deleteOne(Filters.eq("_id", idHex));
            return true;
        }
    }
    
    
    
    public List<Document> listByQuizAndStudent(String quizIdHex, String studentId) {
    List<Document> result = new ArrayList<>();
    try {
        // quizId stored as ObjectId
        result = coll.find(Filters.and(
                Filters.eq("quizId", new ObjectId(quizIdHex)),
                Filters.eq("studentId", studentId)
        )).into(new ArrayList<>());
    } catch (IllegalArgumentException ex) {
        // quizId stored as plain string
        result = coll.find(Filters.and(
                Filters.eq("quizId", quizIdHex),
                Filters.eq("studentId", studentId)
        )).into(new ArrayList<>());
    }
    return result;
}

}
