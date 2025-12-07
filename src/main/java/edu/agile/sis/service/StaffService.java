package edu.agile.sis.service;


import edu.agile.sis.dao.StaffDAO;
import org.bson.Document;

import java.util.List;

public class StaffService {
    private final StaffDAO dao = new StaffDAO();


    public void createStaff(Document staff) {
        dao.insertStaff(staff);
    }

    public List<Document> listAll() {
        return dao.findAll();
    }

    public Document getById(String id) {
        return dao.findById(id);
    }

    public Document getByStaffId(String staffId) {
        return dao.findByStaffId(staffId);
    }

    public void update(String id, Document data) {
        dao.update(id, data);
    }

    public void delete(String id) {
        dao.delete(id);
    }
    
    
    public boolean deleteByStaffId(String staffId) {
    if (staffId == null || staffId.isBlank()) return false;
    return dao.deleteByStaffId(staffId);
    }



    /**
     * Assign staff to a course (simple assignment: push staffId into course.assignedStaff array)
     */
    /*
     * 
     
    public boolean assignToCourse(String courseCode, String staffId) {
        Document course = courseDAO.findByCode(courseCode);
        if (course == null) return false;

        @SuppressWarnings("unchecked")
        List<String> assigned = (List<String>) course.get("assignedStaff");
        if (assigned == null) assigned = new java.util.ArrayList<>();

        if (!assigned.contains(staffId)) assigned.add(staffId);

        courseDAO.update(course.getObjectId("_id").toHexString(),
                new Document("assignedStaff", assigned));
        return true;
    }
        */
}
