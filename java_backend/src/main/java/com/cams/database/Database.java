package com.cams.database;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.cdimascio.dotenv.Dotenv;

public class Database {

    private static final Dotenv dotenv = loadDotenvSafely();
    private static volatile boolean loggedEnvInfo = false;
    private static volatile String cachedJdbcUrl = null;
    private static volatile String cachedUser = null;
    private static volatile String cachedPassword = null;

    private static Dotenv loadDotenvSafely() {
        try {
            return Dotenv.configure().ignoreIfMissing().load();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isRailway() {
        return System.getenv("RAILWAY_ENVIRONMENT") != null ||
               System.getenv("RAILWAY_SERVICE_ID") != null ||
               System.getenv("RAILWAY_PROJECT_ID") != null ||
               System.getenv("RAILWAY_DEPLOYMENT_ID") != null ||
               System.getenv("RAILWAY_STATIC_URL") != null ||
               System.getenv("RAILWAY_PUBLIC_DOMAIN") != null ||
               System.getenv("RAILWAY_PRIVATE_DOMAIN") != null ||
               System.getenv("RAILWAY_GIT_COMMIT_SHA") != null ||
               (System.getenv("PORT") != null && !"5000".equals(System.getenv("PORT")));
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

    private static void logEnvironmentInfoOnce() {
        if (loggedEnvInfo) return;
        synchronized (Database.class) {
            if (loggedEnvInfo) return;
            loggedEnvInfo = true;

            List<String> matched = new ArrayList<>();
            for (String k : System.getenv().keySet()) {
                String u = k.toUpperCase();
                if (u.contains("POSTGRES") || u.contains("DATABASE") || u.contains("PG") ||
                    u.contains("DB") || u.contains("RAILWAY") || u.contains("PORT")) {
                    matched.add(k);
                }
            }
            Collections.sort(matched);
            System.out.println("[Database] Detected environment variables: " + matched);
            System.out.println("[Database] Environment mode: " + (isRailway() ? "Railway Production" : "Local Development"));
        }
    }

    private static String findDatabaseUrlFromEnv() {
        String[] preferredKeys = {
            "DATABASE_PRIVATE_URL",
            "DATABASE_URL",
            "POSTGRES_URL",
            "POSTGRES_PRIVATE_URL",
            "POSTGRESQL_URL",
            "DATABASE_PUBLIC_URL",
            "CAMS_DB_URL",
            "CAMS_DATABASE_URL",
            "DB_URL",
            "SPRING_DATASOURCE_URL"
        };
        for (String key : preferredKeys) {
            String val = getEnv(key);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        }

        // Scan all system environment variables for postgres connection strings
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String val = entry.getValue();
            if (val != null) {
                val = val.trim();
                if (val.startsWith("postgres://") || val.startsWith("postgresql://") || val.startsWith("jdbc:postgresql:")) {
                    return val;
                }
            }
        }

        return null;
    }

    public static Connection getConnection() throws SQLException {
        logEnvironmentInfoOnce();

        if (cachedJdbcUrl != null) {
            try {
                if (cachedUser != null && cachedPassword != null) {
                    return DriverManager.getConnection(cachedJdbcUrl, cachedUser, cachedPassword);
                }
                return DriverManager.getConnection(cachedJdbcUrl);
            } catch (SQLException e) {
                cachedJdbcUrl = null;
            }
        }

        // 1. Try resolving full database URL (preferred on Railway)
        String rawUrl = findDatabaseUrlFromEnv();
        if (rawUrl != null && !rawUrl.isEmpty()) {
            try {
                return connectUsingUrl(rawUrl);
            } catch (SQLException e) {
                System.err.println("[Database] Connection using URL failed: " + e.getMessage());
            }
        }

        // 2. Try resolving individual environment variables
        return connectUsingIndividualVariables();
    }

    private static Connection connectUsingUrl(String url) throws SQLException {
        if (url.startsWith("jdbc:postgresql:")) {
            String user = resolveUser();
            String password = resolvePassword();
            if (user != null && password != null) {
                return DriverManager.getConnection(url, user, password);
            }
            return DriverManager.getConnection(url);
        }

        String clean = url.replaceFirst("^(jdbc:)?postgres(ql)?://", "");
        String query = "";
        int qIdx = clean.indexOf('?');
        if (qIdx >= 0) {
            query = clean.substring(qIdx + 1);
            clean = clean.substring(0, qIdx);
        }

        String dbName = "";
        int slashIdx = clean.indexOf('/');
        if (slashIdx >= 0) {
            dbName = clean.substring(slashIdx + 1);
            clean = clean.substring(0, slashIdx);
        }

        String user = null;
        String password = null;
        String hostPort = clean;
        int atIdx = clean.lastIndexOf('@');
        if (atIdx >= 0) {
            String userPass = clean.substring(0, atIdx);
            hostPort = clean.substring(atIdx + 1);
            int colonIdx = userPass.indexOf(':');
            if (colonIdx >= 0) {
                user = userPass.substring(0, colonIdx);
                password = userPass.substring(colonIdx + 1);
            } else {
                user = userPass;
            }
        }

        if (user != null) {
            try { user = URLDecoder.decode(user, StandardCharsets.UTF_8); } catch (Exception ignored) {}
        }
        if (password != null) {
            try { password = URLDecoder.decode(password, StandardCharsets.UTF_8); } catch (Exception ignored) {}
        }

        String host = hostPort;
        int port = 5432;
        int colonIdx = hostPort.lastIndexOf(':');
        if (colonIdx >= 0) {
            host = hostPort.substring(0, colonIdx);
            try {
                port = Integer.parseInt(hostPort.substring(colonIdx + 1));
            } catch (NumberFormatException ignored) {}
        }

        if (user == null) user = resolveUser();
        if (password == null) password = resolvePassword();
        if (dbName == null || dbName.isEmpty()) dbName = isRailway() ? "railway" : "cams";

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        if (!query.isEmpty()) {
            jdbcUrl += "?" + query;
        }

        System.out.println("[Database] Connecting via URL -> Host: " + host + ", Port: " + port + ", DB: " + dbName + ", User: " + user);
        Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
        cachedJdbcUrl = jdbcUrl;
        cachedUser = user;
        cachedPassword = password;
        return conn;
    }

    private static Connection connectUsingIndividualVariables() throws SQLException {
        List<String> hostCandidates = resolveHostCandidates();
        int port = resolvePort();
        List<String> dbCandidates = resolveDbCandidates();
        String user = resolveUser();
        String password = resolvePassword();

        SQLException lastException = null;

        for (String host : hostCandidates) {
            for (String dbName : dbCandidates) {
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                System.out.println("[Database] Connecting -> Host: " + host + ", Port: " + port + ", DB: " + dbName + ", User: " + user);
                try {
                    Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
                    cachedJdbcUrl = jdbcUrl;
                    cachedUser = user;
                    cachedPassword = password;
                    return conn;
                } catch (SQLException e) {
                    lastException = e;
                    System.err.println("[Database] Failed connecting to " + host + ":" + port + "/" + dbName + ": " + e.getMessage());
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }
        throw new SQLException("Could not connect to PostgreSQL: No valid host or database candidates.");
    }

    private static List<String> resolveHostCandidates() {
        List<String> hosts = new ArrayList<>();
        String[] hostKeys = {
            "PGHOST",
            "POSTGRES_HOST",
            "POSTGRESQL_HOST",
            "DATABASE_HOST",
            "DB_HOST",
            "PG_HOST",
            "CAMS_DB_HOST",
            "RAILWAY_TCP_PROXY_DOMAIN"
        };
        for (String k : hostKeys) {
            String h = getEnv(k);
            if (h != null && !h.isEmpty() && !hosts.contains(h)) {
                hosts.add(h);
            }
        }

        if (isRailway()) {
            // Internal Railway Private Network hosts (never localhost on Railway)
            String[] internalHosts = {
                "postgres.railway.internal",
                "postgresql.railway.internal",
                "database.railway.internal"
            };
            for (String ih : internalHosts) {
                if (!hosts.contains(ih)) {
                    hosts.add(ih);
                }
            }
        } else {
            if (hosts.isEmpty()) {
                hosts.add("localhost");
            }
        }

        return hosts;
    }

    private static int resolvePort() {
        String[] portKeys = {
            "PGPORT",
            "POSTGRES_PORT",
            "POSTGRESQL_PORT",
            "DATABASE_PORT",
            "DB_PORT",
            "PG_PORT",
            "CAMS_DB_PORT",
            "RAILWAY_TCP_PROXY_PORT"
        };
        for (String k : portKeys) {
            String p = getEnv(k);
            if (p != null && !p.isEmpty()) {
                try {
                    return Integer.parseInt(p);
                } catch (NumberFormatException ignored) {}
            }
        }
        return 5432;
    }

    private static List<String> resolveDbCandidates() {
        List<String> dbs = new ArrayList<>();
        String[] dbKeys = {
            "PGDATABASE",
            "POSTGRES_DB",
            "POSTGRES_DATABASE",
            "POSTGRESQL_DATABASE",
            "POSTGRESQL_DB",
            "DATABASE_NAME",
            "DB_NAME",
            "CAMS_DB_NAME"
        };
        for (String k : dbKeys) {
            String d = getEnv(k);
            if (d != null && !d.isEmpty() && !dbs.contains(d)) {
                dbs.add(d);
            }
        }

        if (isRailway()) {
            if (!dbs.contains("railway")) dbs.add("railway");
            if (!dbs.contains("cams")) dbs.add("cams");
            if (!dbs.contains("postgres")) dbs.add("postgres");
        } else {
            if (dbs.isEmpty()) {
                dbs.add("cams");
            }
        }

        return dbs;
    }

    private static String resolveUser() {
        String[] userKeys = {
            "PGUSER",
            "POSTGRES_USER",
            "POSTGRESQL_USER",
            "DATABASE_USER",
            "DB_USER",
            "PG_USER",
            "CAMS_DB_USER"
        };
        for (String k : userKeys) {
            String u = getEnv(k);
            if (u != null && !u.isEmpty()) return u;
        }
        return "postgres";
    }

    private static String resolvePassword() {
        String[] passKeys = {
            "PGPASSWORD",
            "POSTGRES_PASSWORD",
            "POSTGRESQL_PASSWORD",
            "DATABASE_PASSWORD",
            "DB_PASSWORD",
            "PG_PASSWORD",
            "CAMS_DB_PASSWORD"
        };
        for (String k : passKeys) {
            String p = getEnv(k);
            if (p != null && !p.isEmpty()) return p;
        }
        return null;
    }
}