package edu.agile.sis.dao;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AnnouncementDAO {
    private final MongoCollection<Document> coll;

    public AnnouncementDAO() {
        this.coll = DBConnection.getInstance().getDatabase().getCollection("announcements");
        // ensure indexes (run once)
        coll.createIndex(Indexes.descending("isPinned", "pinnedAt", "createdAt"));
        coll.createIndex(Indexes.ascending("category"));
    }

    public String insert(Document doc) {
        doc.append("createdAt", new Date());
        coll.insertOne(doc);
        Object id = doc.get("_id");
        return id == null ? null : id.toString();
    }

    public List<Document> list(int skip, int limit, String category, boolean pinnedFirst) {
        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("deleted", false));
        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) {
            filters.add(Filters.eq("category", category));
        }
        Bson query = filters.size() == 1 ? filters.get(0) : Filters.and(filters);
        FindIterable<Document> it;
        if (pinnedFirst) {
            it = coll.find(query).sort(Sorts.orderBy(Sorts.descending("isPinned"), Sorts.descending("pinnedAt"), Sorts.descending("createdAt")));
        } else {
            it = coll.find(query).sort(Sorts.descending("createdAt"));
        }
        return it.skip(skip).limit(limit).into(new ArrayList<>());
    }

    public Document findById(String idHex) {
        try {
            return coll.find(Filters.eq("_id", new ObjectId(idHex))).first();
        } catch (Exception ex) {
            return coll.find(Filters.eq("_id", idHex)).first();
        }
    }

    public boolean update(String idHex, Document updates) {
        try {
            coll.updateOne(Filters.eq("_id", new ObjectId(idHex)), new Document("$set", updates));
            return true;
        } catch (Exception ex) {
            coll.updateOne(Filters.eq("_id", idHex), new Document("$set", updates));
            return true;
        }
    }

    public void delete(String idHex) {
        try {
            coll.updateOne(Filters.eq("_id", new ObjectId(idHex)), new Document("$set", new Document("deleted", true)));
        } catch (Exception ex) {
            coll.updateOne(Filters.eq("_id", idHex), new Document("$set", new Document("deleted", true)));
        }
    }
}
