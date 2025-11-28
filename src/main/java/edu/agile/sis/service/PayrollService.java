package edu.agile.sis.service;

import edu.agile.sis.dao.PayrollDAO;
import org.bson.Document;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PayrollService {
    private final PayrollDAO dao = new PayrollDAO();


    public boolean createPayroll(String staffId, Document doc) {
        try {
     
            doc.put("staffId", staffId);

    
            String payslipId = "PS-" + UUID.randomUUID();
            doc.put("payslipId", payslipId);

 
            doc.put("createdAt", new Date());


            if (doc.containsKey("filePath")) {
             

                String path = doc.getString("filePath");
                File f = new File(path);
                if (f.exists()) {
                    doc.put("fileName", f.getName());
                }
            }

            dao.insert(doc);
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }



    public Document uploadPayroll(Document p) {
        p.append("payslipId", "PS-" + UUID.randomUUID());
        p.append("createdAt", new Date());
        return dao.insert(p);
    }


    public List<Document> listPayslipsForStaff(String staffId) {
        return dao.findByStaffId(staffId);
    }

    public Document getPayslipById(String id) {
        return dao.findById(id);
    }

    public boolean deletePayroll(String id) {
        return dao.deleteById(id);
    }
    
    
    public boolean updatePayroll(String id, Document updated) {
    return dao.update(id, updated);
}

    
    
    

public List<Document> listAllPayroll() {
    return dao.findAll(); 
}

}
