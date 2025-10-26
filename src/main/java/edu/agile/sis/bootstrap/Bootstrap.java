package edu.agile.sis.bootstrap;

import edu.agile.sis.config.ConfigManager;
import edu.agile.sis.db.DBConnection;
import edu.agile.sis.service.AuthService;

public class Bootstrap {
    public static void main(String[] args) {
        System.out.println("[Bootstrap] Starting");

        ConfigManager cfg = ConfigManager.getInstance();

        // If you want to override programmatically, uncomment and set the URI (no angle brackets)
         cfg.set("mongodb.uri", "mongodb+srv://ahmedtobch2002:Tobch2002@cluster0.tst84cj.mongodb.net/agile_sis_db?retryWrites=true&w=majority");
         cfg.set("mongodb.database", "agile_sis_db");
        try {
            cfg.save();
        } catch (Exception ex) {
            System.err.println("[Bootstrap] Failed to save config: " + ex.getMessage());
        }

        System.out.println("[Bootstrap] Using URI: " + cfg.get("mongodb.uri", "(not set)") );
        System.out.println("[Bootstrap] Using DB : " + cfg.get("mongodb.database", "(not set)") );

        try {
            DBConnection.getInstance().connectFromConfig();
            System.out.println("[Bootstrap] DB connected successfully.");
        } catch (Exception ex) {
            System.err.println("[Bootstrap] DB connection failed: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        System.out.println("[Bootstrap] Done.");
    }
}
