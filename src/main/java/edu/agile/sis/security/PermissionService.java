package edu.agile.sis.security;

import edu.agile.sis.dao.CourseDAO;
import org.bson.Document;

import java.util.List;

public class PermissionService {
    private final CourseDAO courseDAO = new CourseDAO();

    /**
     * Student may message a staff member if there exists at least one course
     * where staffId appears in course.assignedStaff.
     *
     * Note: for a stricter check, also verify student enrollment in that course.
     */
    public boolean studentCanMessageStaff(String studentEntityId, String staffId){
        List<Document> courses = courseDAO.findAll();
        if (courses == null) return false;
        for (Document c : courses) {
            Object assignedObj = c.get("assignedStaff");
            if (assignedObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> assigned = (List<Object>) assignedObj;
                for (Object o : assigned) {
                    if (o != null && o.toString().equals(staffId)) {
                        // permissive: allow messaging if staff assigned to any course
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
