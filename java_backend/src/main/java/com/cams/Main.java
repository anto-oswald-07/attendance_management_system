package com.cams;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

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
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable '" + portEnv + "', defaulting to 5000.");
            }
        }

        CountDownLatch keepAliveLatch = new CountDownLatch(1);

        try {
            HttpApiServer server = new HttpApiServer(port);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down CAMS Backend Server...");
                server.stop();
                keepAliveLatch.countDown();
            }));

            // Test and verify database connection
            testDatabaseConnection();

            // Block main thread indefinitely to keep container process running
            keepAliveLatch.await();
        } catch (Exception e) {
            System.err.println("Fatal error in CAMS Backend Server: " + e.getMessage());
            e.printStackTrace();
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