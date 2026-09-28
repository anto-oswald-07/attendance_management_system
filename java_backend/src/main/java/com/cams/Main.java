package com.cams;

import java.sql.Connection;
import java.sql.SQLException;

import com.cams.database.Database;
import com.cams.server.HttpApiServer;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting CAMS Backend Server...");

        int port = 5000;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portEnv.trim());
                System.out.println("[Config] Detected Railway PORT environment variable: " + port);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable '" + portEnv + "', defaulting to 5000.");
            }
        } else {
            System.out.println("[Config] No PORT environment variable detected, defaulting to 5000.");
        }

        try {
            HttpApiServer server = new HttpApiServer(port);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[Process Event] Shutdown hook triggered: JVM received termination signal (SIGTERM/SIGINT) from Railway container supervisor.");
                server.stop();
            }));

            // Test and verify database connection
            try {
                testDatabaseConnection();
            } catch (Throwable t) {
                System.err.println("[Database Warning] Database initial test threw: " + t.getMessage());
            }

            // Keep main thread alive indefinitely while the HTTP server is running
            System.out.println("[Server Ready] Backend initialized and running on port " + port + ". Main thread entering keep-alive loop.");
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    System.out.println("[Process Event] Main keep-alive thread interrupted: " + e.getMessage());
                    break;
                }
            }
            System.out.println("[Process Event] Main loop terminated.");
        } catch (Throwable t) {
            System.err.println("[Process Event] Fatal error in CAMS Backend Server: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void testDatabaseConnection() {
        int maxRetries = Database.isRailway() ? 5 : 1;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection connection = Database.getConnection()) {
                System.out.println("Database connection established successfully on attempt " + attempt + ".");
                return;
            } catch (SQLException e) {
                System.err.println("Database connection attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                        break;
                    }
                } else {
                    System.err.println("Continuing server operation; subsequent requests will retry database connection.");
                }
            }
        }
    }
}