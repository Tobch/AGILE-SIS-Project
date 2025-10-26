package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class AttributeMetaDAO {
    private final MongoCollection<Document> coll;

    public AttributeMetaDAO(){
        this.coll = DBConnection.getInstance().getDatabase().getCollection("attribute_meta");
    }

    public void insert(Document meta){
        coll.insertOne(meta);
    }

    public List<Document> findAll(){
        return coll.find().into(new ArrayList<>());
    }

    public void update(String key, Document updated){
        coll.updateOne(new Document("key", key), new Document("$set", updated));
    }

    public void delete(String key){
        coll.deleteOne(new Document("key", key));
    }

    public Document findByKey(String key){
        return coll.find(new Document("key", key)).first();
    }
}
