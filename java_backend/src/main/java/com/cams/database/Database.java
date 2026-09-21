package com.cams.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

public class Database {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String URL =
            "jdbc:postgresql://localhost:5432/cams";

    private static final String USER = dotenv.get("CAMS_DB_USER");

    private static final String PASSWORD = dotenv.get("CAMS_DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }
}