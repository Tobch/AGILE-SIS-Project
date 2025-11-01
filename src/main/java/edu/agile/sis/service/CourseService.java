package edu.agile.sis.service;

import edu.agile.sis.dao.CourseDAO;
import edu.agile.sis.security.AuthSession;
import org.bson.Document;

import java.util.List;

public class CourseService {
    private final CourseDAO dao = new CourseDAO();

    public void createCourse(Document course){
        // allow Admin and Professor to create courses
        if (!AuthSession.getInstance().hasRole("Admin") && !AuthSession.getInstance().hasRole("Professor")) {
            throw new SecurityException("Insufficient privileges to create course");
        }
        dao.insertCourse(course);
    }

    public List<Document> listAll(){ return dao.findAll(); }
    
    public List<Document> listByStaff(String staffId) {
        return dao.findByStaff(staffId);
    }

    
    
    
    

    public Document findByCode(String code){ return dao.findByCode(code); }

    public Document findById(String id){ return dao.findById(id); }

    public void update(String id, Document update){
        // allow Admin and Professor to update courses
        if (!AuthSession.getInstance().hasRole("Admin") && !AuthSession.getInstance().hasRole("Professor")) {
            throw new SecurityException("Insufficient privileges to update course");
        }
        dao.update(id, update);
    }

    public void delete(String id){
        if (!AuthSession.getInstance().hasRole("Admin") && !AuthSession.getInstance().hasRole("Professor")) {
            throw new SecurityException("Insufficient privileges to delete course");
        }
        dao.delete(id);
    }

    /**
     * Assign a staffId to a course code (append if not present). Only Admin allowed.
     */
    public boolean assignStaffToCourse(String courseCode, String staffId) {
        if (!AuthSession.getInstance().hasRole("Admin")) {
            throw new SecurityException("Only Admin can assign staff to course");
        }
        Document course = dao.findByCode(courseCode);
        if (course == null) return false;
        @SuppressWarnings("unchecked")
        List<Object> assigned = (List<Object>) course.get("assignedStaff");
        if (assigned == null) assigned = new java.util.ArrayList<>();
        if (!assigned.contains(staffId)) assigned.add(staffId);
        dao.update(course.getObjectId("_id").toHexString(), new Document("assignedStaff", assigned));
        return true;
    }
}
