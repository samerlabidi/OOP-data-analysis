package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import exception.ConfigurationException;

public class AppConfig {
    private static final Properties props = new Properties();
    
    static {
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration", e);
        }
    }
    
    public static String getDatabaseUrl() {
        return props.getProperty("db.url");
    }
    
    public static String getDatabaseUser() {
        return props.getProperty("db.user");
    }
    
    public static String getDatabasePassword() {
        return props.getProperty("db.password");
    }
} 