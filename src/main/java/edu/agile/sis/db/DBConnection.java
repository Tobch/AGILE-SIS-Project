package edu.agile.sis.db;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import edu.agile.sis.config.ConfigManager;

public class DBConnection {
    private static DBConnection INSTANCE;
    private MongoClient client;
    private MongoDatabase database;

    private DBConnection(){}

    public static synchronized DBConnection getInstance() {
        if(INSTANCE == null) INSTANCE = new DBConnection();
        return INSTANCE;
    }

    public void connectFromConfig() {
        ConfigManager cfg = ConfigManager.getInstance();
        String uri = cfg.get("mongodb.uri", "mongodb://localhost:27017");
        String dbName = cfg.get("mongodb.database", "agile_sis_db");
        connect(uri, dbName);
    }

    public void connect(String uri, String dbName){
        if(client != null) client.close();
        client = MongoClients.create(new ConnectionString(uri));
        database = client.getDatabase(dbName);
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        if(client != null) client.close();
    }
}
