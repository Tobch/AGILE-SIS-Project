package edu.agile.sis.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import edu.agile.sis.db.DBConnection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final MongoCollection<Document> users;

    public UserDAO() {
        this.users = DBConnection.getInstance().getDatabase().getCollection("users");
    }

    public void insertUser(String username, String passwordHash, List<String> roles, String linkedEntityId){
        Document doc = new Document("username", username)
                .append("passwordHash", passwordHash)
                .append("roles", roles)
                .append("linkedEntityId", linkedEntityId)
                .append("createdAt", new java.util.Date());
        users.insertOne(doc);
    }

    public Document findByUsername(String username){
        return users.find(Filters.eq("username", username)).first();
    }

    public List<Document> findAllUsers(){
        return users.find().into(new ArrayList<>());
    }

    public void updatePassword(String username, String newHash){
        users.updateOne(Filters.eq("username", username),
                new Document("$set", new Document("passwordHash", newHash)));
    }

    public void deleteUser(String username){
        users.deleteOne(Filters.eq("username", username));
    }
}
