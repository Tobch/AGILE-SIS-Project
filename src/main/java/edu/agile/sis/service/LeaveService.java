package edu.agile.sis.service;

import edu.agile.sis.dao.LeaveDAO;
import org.bson.Document;

import java.util.List;

public class LeaveService {
    private final LeaveDAO dao = new LeaveDAO();

   
    public Document createLeave(Document d) {

        if (!d.containsKey("status")) d.append("status", "PENDING");
        if (!d.containsKey("createdAt")) d.append("createdAt", System.currentTimeMillis());
        return dao.insert(d);
    }

  
    public List<Document> listAllLeaves() {
        return dao.findAll();
    }


    public List<Document> listLeavesForStaff(String staffId) {
        return dao.findByStaffId(staffId);
    }


    public Document getLeaveById(String id) {
        return dao.findById(id);
    }

    
    public boolean updateLeave(String id, Document updated) {
        return dao.update(id, updated);
    }

    
    public boolean updateStatus(String id, String status, String approver, String note) {
        return dao.updateStatus(id, status, approver, note);
    }

    
    public boolean deleteLeave(String id) {
        return dao.deleteById(id);
    }
}
