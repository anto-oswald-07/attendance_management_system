package com.cams.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cams.database.Database;
import com.cams.model.Student;

public class StudentRepository {

    private UserRepository userRepository = new UserRepository();

    public boolean addStudent(String username, String password, String name, String rollNo, String division) {
        String studentSql = """
                INSERT INTO students (user_id, name, roll_no, division)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int userId = userRepository.insertUser(connection, username, password, "STUDENT");
                if (userId <= 0) {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement statement = connection.prepareStatement(studentSql)) {
                    statement.setInt(1, userId);
                    statement.setString(2, name);
                    statement.setString(3, rollNo);
                    statement.setString(4, division);
                    statement.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getAllStudentsWithStats() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT s.id as student_id, s.user_id, s.name, s.roll_no, s.division, u.username
                FROM students s
                JOIN users u ON s.user_id = u.id
                ORDER BY s.id ASC
                """;

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                int studentId = resultSet.getInt("student_id");
                int userId = resultSet.getInt("user_id");
                String name = resultSet.getString("name");
                String rollNo = resultSet.getString("roll_no");
                String division = resultSet.getString("division");
                String username = resultSet.getString("username");

                double pct = getAttendancePercentage(connection, studentId);
                String status = pct < 75.0 ? "Shortage Alert" : "Good Standing";

                Map<String, Object> map = new HashMap<>();
                map.put("studentId", studentId);
                map.put("userId", userId);
                map.put("id", rollNo);
                map.put("rollNo", rollNo);
                map.put("name", name);
                map.put("department", division);
                map.put("semester", "Semester 6");
                map.put("email", username.contains("@") ? username : rollNo.toLowerCase() + "@college.edu");
                map.put("attendancePct", pct);
                map.put("status", status);
                list.add(map);
            }
        } catch (SQLException e) {
            System.out.println("Failed to retrieve students.");
            e.printStackTrace();
        }
        return list;
    }

    public Student findStudentByUserId(int userId) {
        String sql = """
                SELECT s.id, s.name, s.roll_no, s.division, u.username, u.password, u.role
                FROM students s
                JOIN users u ON s.user_id = u.id
                WHERE s.user_id = ?
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            userId,
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("name"),
                            rs.getString("roll_no"),
                            rs.getString("division")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Student findStudentByRollNo(String rollNo) {
        String sql = """
                SELECT s.id, s.user_id, s.name, s.roll_no, s.division, u.username, u.password, u.role
                FROM students s
                JOIN users u ON s.user_id = u.id
                WHERE s.roll_no = ?
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, rollNo);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("name"),
                            rs.getString("roll_no"),
                            rs.getString("division")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getStudentTableIdByRollNo(String rollNo) {
        String sql = "SELECT id FROM students WHERE roll_no = ?";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, rollNo);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int getStudentTableIdByUserId(int userId) {
        String sql = "SELECT id FROM students WHERE user_id = ?";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<Map<String, Object>> getStudentsByDivision(String division) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT id, user_id, name, roll_no, division
                FROM students
                WHERE division = ?
                ORDER BY roll_no ASC
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, division);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("userId", rs.getInt("user_id"));
                    map.put("name", rs.getString("name"));
                    map.put("rollNo", rs.getString("roll_no"));
                    map.put("division", rs.getString("division"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteStudentByRollNo(String rollNo) {
        String sql = """
                DELETE FROM users
                WHERE id = (SELECT user_id FROM students WHERE roll_no = ?)
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, rollNo);
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getStudentCount() {
        String sql = "SELECT count(*) FROM students";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery()
        ) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getAttendancePercentage(Connection connection, int studentId) {
        String sql = """
                SELECT count(*) as total, count(CASE WHEN status = 'PRESENT' THEN 1 END) as attended
                FROM attendance
                WHERE student_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int attended = rs.getInt("attended");
                    if (total == 0) {
                        return 100.0;
                    }
                    return Math.round(((double) attended / total * 100.0) * 10.0) / 10.0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 100.0;
    }
}
