package edu.agile.sis.config;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static ConfigManager INSTANCE;
    private final Properties props = new Properties();
    private final String configFile = "config.properties";

    private ConfigManager() {
        try {
            File f = new File(configFile);
            if(f.exists()) {
                try (InputStream is = new FileInputStream(f)) {
                    props.load(is);
                }
            }
        } catch (IOException ignored) {}
    }

    public static synchronized ConfigManager getInstance() {
        if(INSTANCE == null) INSTANCE = new ConfigManager();
        return INSTANCE;
    }

    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
    }

    public void save() throws IOException {
        try(OutputStream os = new FileOutputStream(configFile)) {
            props.store(os, "AGILE SIS Config");
        }
    }
}
