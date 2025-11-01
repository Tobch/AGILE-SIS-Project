package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class StaffDAO {
    private final MongoCollection<Document> staffColl;

    public StaffDAO(){
        this.staffColl = DBConnection.getInstance().getDatabase().getCollection("staff");
    }

    public void insertStaff(Document staff){
        staffColl.insertOne(staff);
    }

    public List<Document> findAll(){
        return staffColl.find().into(new ArrayList<>());
    }

    public Document findById(String id){
        try {
            return staffColl.find(Filters.eq("_id", new ObjectId(id))).first();
        } catch (IllegalArgumentException ex){
            return staffColl.find(Filters.eq("_id", id)).first();
        }
    }

    public Document findByStaffId(String staffId){
        return staffColl.find(Filters.eq("staffId", staffId)).first();
    }

    public void update(String id, Document updated){
        try {
            staffColl.updateOne(Filters.eq("_id", new ObjectId(id)), new Document("$set", updated));
        } catch (IllegalArgumentException ex) {
            staffColl.updateOne(Filters.eq("_id", id), new Document("$set", updated));
        }
    }

    public void delete(String id){
        try {
            staffColl.deleteOne(Filters.eq("_id", new ObjectId(id)));
        } catch (IllegalArgumentException ex) {
            staffColl.deleteOne(Filters.eq("_id", id));
        }
    }
    public boolean deleteByStaffId(String staffId) {
    var result = staffColl.deleteOne(Filters.eq("staffId", staffId));
    return result.getDeletedCount() > 0;
    }


    
    
    
}
