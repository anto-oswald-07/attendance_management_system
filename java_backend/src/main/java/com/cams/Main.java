package com.cams;

import java.sql.Connection;
import java.sql.SQLException;

import com.cams.database.Database;
import com.cams.server.HttpApiServer;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting CAMS Backend Server...");

        int maxRetries = Database.isRailway() ? 5 : 1;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try (Connection connection = Database.getConnection()) {
                System.out.println("Database connection established successfully on attempt " + attempt + ".");
                break;
            } catch (SQLException e) {
                System.err.println("Database connection attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                } else {
                    System.err.println("Continuing server startup; subsequent requests will retry database connection.");
                }
            }
        }

        int port = 5000;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable '" + portEnv + "', defaulting to 5000.");
            }
        }

        try {
            HttpApiServer server = new HttpApiServer(port);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down CAMS Backend Server...");
                server.stop();
            }));

            // Keep main thread alive so the container process remains running
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}