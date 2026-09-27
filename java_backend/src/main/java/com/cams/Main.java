package com.cams;

import java.sql.Connection;
import java.sql.SQLException;

import com.cams.database.Database;
import com.cams.server.HttpApiServer;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting CAMS Backend Server...");

        try (Connection connection = Database.getConnection()) {
            System.out.println("Database connection established successfully.");
        } catch (SQLException e) {
            System.err.println("Database connection failed. Please ensure PostgreSQL is running.");
            e.printStackTrace();
        }

        try {
            HttpApiServer server = new HttpApiServer(5000);
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}