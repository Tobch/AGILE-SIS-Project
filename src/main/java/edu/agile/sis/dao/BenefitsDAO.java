package edu.agile.sis.dao;

import edu.agile.sis.db.DBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class BenefitsDAO {
    private final MongoCollection<Document> col = DBConnection.getInstance().getDatabase().getCollection("benefits");

    public Document insert(Document d) { col.insertOne(d); return d; }
    public List<Document> findByStaff(String staffId) { List<Document> out = new ArrayList<>(); col.find(Filters.eq("staffId", staffId)).into(out); return out; }
 
    public boolean update(String id, Document patch) { var r = col.updateOne(Filters.eq("_id", new ObjectId(id)), new Document("$set", patch)); return r.getModifiedCount() > 0; }
     
    public List<Document> findAll() {
    var out = new ArrayList<Document>();
    col.find().into(out);
    return out;
}


public boolean deleteById(String id) {
    return col.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
}
}
