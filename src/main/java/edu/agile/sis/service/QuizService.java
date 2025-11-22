package edu.agile.sis.service;

import edu.agile.sis.dao.QuizAttemptDAO;
import edu.agile.sis.dao.QuizDAO;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;

/**
 * QuizService - business logic for quizzes and attempts (auto-grading for MCQ).
 *
 * Improvements:
 * - store quizId in attempt using the same type as quiz._id (ObjectId when quiz._id is ObjectId)
 * - mark graded=false if any text questions present
 * - gradeAttempt recalculates percent using maxScore
 */
public class QuizService {
    private final QuizDAO quizDAO = new QuizDAO();
    private final QuizAttemptDAO attemptDAO = new QuizAttemptDAO();
    private final EntityService entityService = new EntityService("students");

    public QuizService() {}

    // CRUD for quizzes
    public String createQuiz(String courseCode, String title, int timeLimitMinutes, List<Document> questions, String createdBy) {
        Document quiz = new Document("courseCode", courseCode)
                .append("title", title)
                .append("timeLimitMinutes", timeLimitMinutes)
                .append("questions", questions == null ? List.of() : questions)
                .append("createdBy", createdBy)
                .append("createdAt", new Date());
        return quizDAO.insert(quiz);
    }

    public List<Document> listByCourse(String courseCode) {
        return quizDAO.listByCourse(courseCode);
    }

    public Document getQuizById(String id) {
        return quizDAO.findById(id);
    }

    public boolean updateQuiz(String id, Document updates) {
        return quizDAO.update(id, updates);
    }

    public boolean deleteQuiz(String id) {
        return quizDAO.delete(id);
    }
    
    
    
    /**
 * Check if a student already has an attempt for a given quiz.
 */
public boolean hasAttempt(String quizId, String studentId) {
    List<Document> attempts = attemptDAO.listByQuiz(quizId);
    if (attempts == null) return false;
    for (Document a : attempts) {
        String sid = a.getString("studentId");
        if (sid != null && sid.equals(studentId)) {
            return true;
        }
    }
    return false;
}


    // Attempts
    public String submitAttempt(String quizId, String studentId, Map<String, Object> answers) {
         if (hasAttempt(quizId, studentId)) {
        throw new IllegalStateException("Student has already submitted an attempt for this quiz.");
    }

    Document quiz = getQuizById(quizId);
    if (quiz == null) throw new IllegalArgumentException("Quiz not found");
        
        

        // auto-grade: compute score and max
        List<Document> questions = quiz.getList("questions", Document.class, List.of());
        double max = questions.size() * 1.0; // each question = 1 point by default
        double score = 0.0;
        boolean hasTextQuestion = false;

        for (Document q : questions) {
            String qid = q.getString("id");
            String type = q.getString("type"); // "mcq" or "text"
            Object given = (answers == null ? null : answers.get(qid));
            if ("mcq".equalsIgnoreCase(type)) {
                Object correct = q.get("correct");
                if (correct != null && given != null) {
                    if (correct.toString().trim().equalsIgnoreCase(given.toString().trim())) {
                        score += 1.0;
                    }
                }
            } else {
                // text / essay type -> require manual grading
                hasTextQuestion = true;
            }
        }

        double percent = (max <= 0) ? 0.0 : (score / max) * 100.0;

        Document attempt = new Document();
        // store quizId using same type as quiz._id if possible
        Object quizIdRaw = quiz.get("_id");
        if (quizIdRaw instanceof ObjectId) {
            attempt.append("quizId", quizIdRaw);
        } else {
            // fallback to the provided quizId string
            attempt.append("quizId", quizId);
        }

        attempt.append("studentId", studentId)
                .append("answers", answers == null ? new Document() : new Document(answers))
                .append("score", score)
                .append("maxScore", max)
                .append("percent", percent)
                .append("submittedAt", new Date())
                .append("graded", !hasTextQuestion) // if there are text questions => not graded automatically
                .append("grader", null)
                .append("feedback", null);

        return attemptDAO.insert(attempt);
    }

    public List<Document> listAttemptsForQuiz(String quizId) {
        return attemptDAO.listByQuiz(quizId);
    }

    public List<Document> listAttemptsForStudent(String studentId) {
        return attemptDAO.listByStudent(studentId);
    }

    public Document getAttemptById(String id) {
        return attemptDAO.findById(id);
    }

    /**
     * Grade (or override) an attempt. Recalculates percent using the stored maxScore.
     *
     * @param attemptId hex string or string id for attempt._id
     * @param score     numeric score (raw points)
     * @param feedback  textual feedback
     * @param grader    username of grader
     * @return true if saved
     */
    public boolean gradeAttempt(String attemptId, double score, String feedback, String grader) {
        // fetch attempt to read maxScore
        Document existing = attemptDAO.findById(attemptId);
        if (existing == null) throw new IllegalArgumentException("Attempt not found");

        double maxScore = 0.0;
        Object mxObj = existing.get("maxScore");
        if (mxObj instanceof Number) maxScore = ((Number) mxObj).doubleValue();
        else {
            try { maxScore = Double.parseDouble(String.valueOf(mxObj)); } catch (Exception ignored) {}
        }

        double percent = (maxScore <= 0) ? 0.0 : (score / maxScore) * 100.0;

        Document upd = new Document("score", score)
                .append("percent", percent)
                .append("feedback", feedback)
                .append("grader", grader)
                .append("graded", true)
                .append("gradedAt", new Date());

        return attemptDAO.update(attemptId, upd);
    }
}
