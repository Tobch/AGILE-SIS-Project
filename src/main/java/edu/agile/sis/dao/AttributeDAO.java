package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class AttributeDAO {
    private final MongoCollection<Document> attrs;

    public AttributeDAO() {
        attrs = DBConnection.getInstance().getDatabase().getCollection("attributes");
    }

    public void insertAttribute(Document attr) {
        attrs.insertOne(attr);
    }

    public Document findByKeyAndType(String key, String entityType) {
        return attrs.find(Filters.and(Filters.eq("key", key), Filters.eq("entityType", entityType))).first();
    }

    public List<Document> listForEntityType(String entityType) {
        return attrs.find(Filters.eq("entityType", entityType)).into(new ArrayList<>());
    }
}
