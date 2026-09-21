package com.cams.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cams.database.Database;


public class UserRepository {

    public void getAllUsers() {
        String sql = "SELECT * FROM users";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String role = resultSet.getString("role");

                System.out.println(
                        id + " | " + username + " | " + role
                );
            }
        } 
        catch (SQLException e) {
            System.out.println("Failed to retrieve users.");
            e.printStackTrace();
        }
    }

    public void createUser(String username, String password, String role) {
        String sql = """
                INSERT INTO users (username, password, role)
                VALUES (?, ?, ?)
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, role);

            statement.executeUpdate();

            System.out.println("User created successfully.");
        } 
        catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    public void findUserByUsername(String username) {
        String sql = """
                SELECT * FROM users
                WHERE username = ?
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    System.out.println(
                            resultSet.getInt("id") + " | "
                                    + resultSet.getString("username") + " | "
                                    + resultSet.getString("role")
                    );
                } else {
                    System.out.println("User not found.");
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Failed to find user.");
            e.printStackTrace();
        }
    }

    public void findUserById(int id) {
        String sql = """
                SELECT * FROM users
                WHERE id = ?
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    System.out.println(
                            resultSet.getInt("id") + " | "
                                    + resultSet.getString("username") + " | "
                                    + resultSet.getString("role")
                    );
                } else {
                    System.out.println("User not found.");
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Failed to find user.");
            e.printStackTrace();
        }
    }

    public void updateUser(int id, String username, String password, String role) {
        String sql = """
                UPDATE users
                SET username = ?, password = ?, role = ?
                WHERE id = ?
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, role);
            statement.setInt(4, id);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User updated successfully.");
            } else {
                System.out.println("User not found.");
            }
        }
        catch (SQLException e) {
            System.out.println("Failed to update user.");
            e.printStackTrace();
        }
    }

    public void deleteUser(int id){
        String sql = """
                DELETE from users
                where id = ?
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setInt(1, id);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("User not found.");
            }
        }
        catch (SQLException e) {
            System.out.println("Failed to delete user.");
            e.printStackTrace();
        }
    }
}