package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class EntityDAO {
    private final MongoCollection<Document> entities;

    public EntityDAO() {
        this.entities = DBConnection.getInstance().getDatabase().getCollection("entities");
    }

    public void insertEntity(Document entity){
        entities.insertOne(entity);
    }

    public Document findByEntityId(String entityId){
        return entities.find(Filters.eq("core.entityId", entityId)).first();
    }

    public List<Document> findByType(String type){
        return entities.find(Filters.eq("type", type)).into(new ArrayList<>());
    }

    public List<Document> findAll(){
        return entities.find().into(new ArrayList<>());
    }

    public void updateEntity(String entityId, Document updatedData){
        entities.updateOne(Filters.eq("core.entityId", entityId),
                new Document("$set", updatedData));
    }

    public void deleteEntity(String entityId){
        entities.deleteOne(Filters.eq("core.entityId", entityId));
    }
}
