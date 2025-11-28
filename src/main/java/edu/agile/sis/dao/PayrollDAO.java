package edu.agile.sis.dao;

import edu.agile.sis.db.DBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;


public class PayrollDAO {

    private final MongoCollection<Document> col =
            DBConnection.getInstance().getDatabase().getCollection("payslips");

  
    public Document insert(Document d) {
        col.insertOne(d);
        return d;
    }

    public List<Document> findByStaffId(String staffId) {
        List<Document> out = new ArrayList<>();
        col.find(Filters.eq("staffId", staffId)).into(out);
        return out;
    }

    
    public Document findById(String id) {
        return col.find(Filters.eq("_id", new ObjectId(id))).first();
    }

    public boolean deleteById(String id) {
        return col.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
    }
    
    
    
    public boolean update(String id, Document d) {
    ObjectId oid = new ObjectId(id);

    Document update = new Document("$set", d);
    return col.updateOne(Filters.eq("_id", oid), update).getModifiedCount() > 0;
}

    
    
 public List<Document> findAll() {
    List<Document> out = new ArrayList<>();
    col.find().sort(Sorts.descending("period", "createdAt")).into(out);
    return out;
 }

}
