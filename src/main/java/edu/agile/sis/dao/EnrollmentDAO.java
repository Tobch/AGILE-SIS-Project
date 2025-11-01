package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * EnrollmentDAO - DAO for enrollments collection.
 * Adds counting helpers used by EnrollmentService.
 */
public class EnrollmentDAO {
    private final MongoCollection<Document> coll;

    public EnrollmentDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("enrollments");
    }

    // ----- core CRUD (original names) -----

    public void insert(Document enrollment) {
        coll.insertOne(enrollment);
    }

    public List<Document> findByStudentId(String studentEntityId) {
        return coll.find(Filters.eq("studentId", studentEntityId)).into(new ArrayList<>());
    }

    public List<Document> findByCourseCode(String courseCode) {
        return coll.find(Filters.eq("courseCode", courseCode)).into(new ArrayList<>());
    }

    public Document findOne(String studentId, String courseCode) {
        return coll.find(Filters.and(Filters.eq("studentId", studentId), Filters.eq("courseCode", courseCode))).first();
    }

    public void deleteById(String id) {
        try {
            coll.deleteOne(Filters.eq("_id", new ObjectId(id)));
        } catch (IllegalArgumentException ex) {
            coll.deleteOne(Filters.eq("_id", id));
        }
    }

    public void delete(String studentId, String courseCode) {
        coll.deleteOne(Filters.and(Filters.eq("studentId", studentId), Filters.eq("courseCode", courseCode)));
    }

    public List<Document> findAll() {
        return coll.find().into(new ArrayList<>());
    }

    // ----- adapter methods expected by EnrollmentService -----

    public void insertEnrollment(Document enrollment) {
        insert(enrollment);
    }

    public List<Document> listByStudent(String studentEntityId) {
        return findByStudentId(studentEntityId);
    }

    public List<Document> listByCourse(String courseCode) {
        return findByCourseCode(courseCode);
    }

    public Document find(String studentId, String courseCode) {
        return findOne(studentId, courseCode);
    }

    public boolean deleteByStudentAndCourse(String studentId, String courseCode) {
        long before = coll.countDocuments(Filters.and(Filters.eq("studentId", studentId), Filters.eq("courseCode", courseCode)));
        delete(studentId, courseCode);
        long after = coll.countDocuments(Filters.and(Filters.eq("studentId", studentId), Filters.eq("courseCode", courseCode)));
        return after < before;
    }

    // ----- new helpers -----

    /**
     * Count all enrollment documents for a student (raw count).
     */
    public long countByStudent(String studentEntityId) {
        return coll.countDocuments(Filters.eq("studentId", studentEntityId));
    }

    /**
     * Count distinct course codes that the student is registered for.
     * This ignores duplicate enrollment documents for the same course.
     */
    public int countDistinctCoursesByStudent(String studentEntityId) {
        List<String> distinct = coll.distinct("courseCode", Filters.eq("studentId", studentEntityId), String.class)
                .into(new ArrayList<>());
        return distinct.size();
    }
}
