package edu.agile.sis.service;

import edu.agile.sis.dao.BenefitsDAO;
import org.bson.Document;

import java.util.List;
import java.util.UUID;




public class BenefitsService {
    private final BenefitsDAO dao = new BenefitsDAO();

    public Document getBenefitsForStaff(String staffId) {
        var list = dao.findByStaff(staffId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Document createOrUpdate(String staffId, Document patch) {
        var current = dao.findByStaff(staffId);
        if (current.isEmpty()) {
            patch.append("benefitId", "B-" + UUID.randomUUID()).append("staffId", staffId).append("createdAt", new java.util.Date());
            return dao.insert(patch);
        } else {
            String id = current.get(0).getObjectId("_id").toHexString();
            dao.update(id, patch);
            return dao.findByStaff(staffId).get(0);
        }
    }
    
    
    
    public List<Document> listAllBenefits() {
   
    return dao.findAll(); 
}

public boolean deleteById(String id) {
    return dao.deleteById(id); 
}


public boolean getDaoDeleteByIdIfExists(String id) {
    return dao.deleteById(id);
}



}
