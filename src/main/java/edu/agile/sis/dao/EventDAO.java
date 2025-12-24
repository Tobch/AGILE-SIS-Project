package edu.agile.sis.dao;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventDAO {
    private final MongoCollection<Document> coll;

    public EventDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("events");
        coll.createIndex(Indexes.ascending("startAt"));
        coll.createIndex(Indexes.ascending("category"));
    }

    public String insert(Document doc) {
        doc.append("createdAt", new Date());
        coll.insertOne(doc);
        Object id = doc.get("_id");
        return id == null ? null : id.toString();
    }

    public List<Document> listUpcoming(Date from, int skip, int limit, String category) {
        Document filter = new Document("deleted", false);
        if (from != null) filter.append("startAt", new Document("$gte", from));
        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) filter.append("category", category);
        FindIterable<Document> it = coll.find(filter).sort(Sorts.ascending("startAt"));
        return it.skip(skip).limit(limit).into(new ArrayList<>());
    }

    public Document findById(String idHex) {
        try { return coll.find(new Document("_id", new ObjectId(idHex))).first(); }
        catch (Exception ex) { return coll.find(new Document("_id", idHex)).first(); }
    }
}
