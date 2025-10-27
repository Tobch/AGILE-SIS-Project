package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class CourseDAO {
    private final MongoCollection<Document> coll;

    public CourseDAO(){
        this.coll = DBConnection.getInstance().getDatabase().getCollection("courses");
    }

    public void insertCourse(Document course){
        coll.insertOne(course);
    }

    public List<Document> findAll(){
        return coll.find().into(new ArrayList<>());
    }

    public Document findByCode(String code){
        return coll.find(Filters.eq("code", code)).first();
    }

    public Document findById(String id){
        try {
            return coll.find(Filters.eq("_id", new ObjectId(id))).first();
        } catch (IllegalArgumentException ex){
            return coll.find(Filters.eq("_id", id)).first();
        }
    }

    public void update(String id, Document updated){
        try {
            coll.updateOne(Filters.eq("_id", new ObjectId(id)), new Document("$set", updated));
        } catch (IllegalArgumentException ex){
            coll.updateOne(Filters.eq("_id", id), new Document("$set", updated));
        }
    }

    public void delete(String id){
        try {
            coll.deleteOne(Filters.eq("_id", new ObjectId(id)));
        } catch (IllegalArgumentException ex){
            coll.deleteOne(Filters.eq("_id", id));
        }
    }

    // 🔹 New method: get courses where staffId is inside assignedStaff array
    public List<Document> findByStaff(String staffId) {
        return coll.find(Filters.in("assignedStaff", staffId)).into(new ArrayList<>());
    }
}
