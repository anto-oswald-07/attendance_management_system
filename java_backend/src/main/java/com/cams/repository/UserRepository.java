package com.cams.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cams.database.Database;
import com.cams.model.Admin;
import com.cams.model.Faculty;
import com.cams.model.Student;
import com.cams.model.User;
import com.cams.security.PasswordHasher;


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
        String hashedPassword = PasswordHasher.hashPassword(password);
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            statement.setString(3, role);

            statement.executeUpdate();

            System.out.println("User created successfully.");
        } 
        catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    public User findUserByUsername(String username) {
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
                    int id = resultSet.getInt("id");
                    String usernameFromDb = resultSet.getString("username");
                    String password = resultSet.getString("password");
                    String role = resultSet.getString("role");
                    if (role.equals("ADMIN")) {
                        return new Admin(id, usernameFromDb, password, role);
                    }
                    else if(role.equals("FACULTY")){
                        String facultySql = """
                                SELECT name, employee_id
                                FROM faculty
                                WHERE user_id = ?;
                                """;
                            
                        try (PreparedStatement facultyStatement = connection.prepareStatement(facultySql)){
                            facultyStatement.setInt(1, id);
                            try(ResultSet facultyResultSet = facultyStatement.executeQuery()){
                                if(facultyResultSet.next()){
                                    String name = facultyResultSet.getString("name");
                                    String employeeId = facultyResultSet.getString("employee_id");
                                    return new Faculty(id, usernameFromDb, password, role, name, employeeId);
                                }
                            }
                        }
                    }
                    else if(role.equals("STUDENT")){
                        String studentSql = """
                                SELECT name, roll_no, division
                                FROM students
                                WHERE user_id = ?;
                                """;
                            
                        try (PreparedStatement studentStatement = connection.prepareStatement(studentSql)){
                            studentStatement.setInt(1, id);
                            try(ResultSet studentResultSet = studentStatement.executeQuery()){
                                if(studentResultSet.next()){
                                    String name = studentResultSet.getString("name");
                                    String rollNo = studentResultSet.getString("roll_no");
                                    String division = studentResultSet.getString("division");
                                    return new Student(id, usernameFromDb, password, role, name, rollNo, division);
                                }
                            }
                        }
                    }
                }
                else {
                    System.out.println("User not found.");
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Failed to find user.");
            e.printStackTrace();
        }
        return null;
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
        String hashedPassword = PasswordHasher.hashPassword(password);
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
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