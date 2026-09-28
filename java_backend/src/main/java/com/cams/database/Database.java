package com.cams.database;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

public class Database {

    private static final Dotenv dotenv = loadDotenvSafely();

    private static Dotenv loadDotenvSafely() {
        try {
            return Dotenv.configure().ignoreIfMissing().load();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String getEnv(String key) {
        // 1. Production Railway environment variables
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 2. JVM System properties
        val = System.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 3. Local development .env file
        if (dotenv != null) {
            try {
                String dotVal = dotenv.get(key);
                if (dotVal != null && !dotVal.trim().isEmpty()) {
                    return dotVal.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static Connection getConnection() throws SQLException {
        // 1. Support full DATABASE_URL / DATABASE_PUBLIC_URL (standard in Railway PostgreSQL)
        String databaseUrl = getEnv("DATABASE_URL");
        if (databaseUrl == null) databaseUrl = getEnv("DATABASE_PUBLIC_URL");
        if (databaseUrl == null) databaseUrl = getEnv("CAMS_DB_URL");

        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            if (databaseUrl.startsWith("jdbc:postgresql:")) {
                String user = getEnv("CAMS_DB_USER");
                if (user == null) user = getEnv("PGUSER");
                String password = getEnv("CAMS_DB_PASSWORD");
                if (password == null) password = getEnv("PGPASSWORD");

                if (user != null && password != null) {
                    return DriverManager.getConnection(databaseUrl, user, password);
                }
                return DriverManager.getConnection(databaseUrl);
            }

            try {
                URI uri = new URI(databaseUrl);
                String userInfo = uri.getUserInfo();
                String user = null;
                String password = null;
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    user = parts[0];
                    password = parts[1];
                }
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + (path != null ? path : "");
                if (query != null && !query.isEmpty()) {
                    jdbcUrl += "?" + query;
                }

                if (user != null && password != null) {
                    return DriverManager.getConnection(jdbcUrl, user, password);
                }
                return DriverManager.getConnection(jdbcUrl);
            } catch (Exception e) {
                String jdbcUrl = databaseUrl.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
                return DriverManager.getConnection(jdbcUrl);
            }
        }

        // 2. Fall back to individual host/port/database environment variables
        String host = getEnv("PGHOST");
        if (host == null) host = getEnv("POSTGRES_HOST");
        if (host == null) host = getEnv("CAMS_DB_HOST");
        if (host == null) host = "localhost";

        String portStr = getEnv("PGPORT");
        if (portStr == null) portStr = getEnv("POSTGRES_PORT");
        if (portStr == null) portStr = getEnv("CAMS_DB_PORT");
        if (portStr == null) portStr = "5432";

        String dbName = getEnv("PGDATABASE");
        if (dbName == null) dbName = getEnv("POSTGRES_DB");
        if (dbName == null) dbName = getEnv("CAMS_DB_NAME");
        if (dbName == null) dbName = "cams";

        String user = getEnv("CAMS_DB_USER");
        if (user == null) user = getEnv("PGUSER");
        if (user == null) user = getEnv("POSTGRES_USER");
        if (user == null) user = "postgres";

        String password = getEnv("CAMS_DB_PASSWORD");
        if (password == null) password = getEnv("PGPASSWORD");
        if (password == null) password = getEnv("POSTGRES_PASSWORD");

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + portStr + "/" + dbName;

        return DriverManager.getConnection(jdbcUrl, user, password);
    }
}