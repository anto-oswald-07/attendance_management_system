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
import com.cams.model.Faculty;

public class FacultyRepository {

    private UserRepository userRepository = new UserRepository();

    public boolean addFaculty(String username, String password, String name, String employeeId, List<String> subjectCodes) {
        String facultySql = """
                INSERT INTO faculty (user_id, name, employee_id)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int userId = userRepository.insertUser(connection, username, password, "FACULTY");
                if (userId <= 0) {
                    connection.rollback();
                    return false;
                }

                int facultyId = -1;
                try (PreparedStatement statement = connection.prepareStatement(facultySql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, userId);
                    statement.setString(2, name);
                    statement.setString(3, employeeId);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            facultyId = keys.getInt(1);
                        }
                    }
                }

                if (facultyId > 0 && subjectCodes != null) {
                    for (String code : subjectCodes) {
                        if (code == null || code.trim().isEmpty()) continue;
                        String assignSql = """
                                INSERT INTO faculty_subjects (faculty_id, subject_id)
                                SELECT ?, id FROM subjects WHERE code = ?
                                ON CONFLICT DO NOTHING
                                """;
                        try (PreparedStatement assignStmt = connection.prepareStatement(assignSql)) {
                            assignStmt.setInt(1, facultyId);
                            assignStmt.setString(2, code.trim());
                            assignStmt.executeUpdate();
                        }
                    }
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

    public List<Map<String, Object>> getAllFacultyWithDetails() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT f.id as faculty_id, f.user_id, f.name, f.employee_id, u.username
                FROM faculty f
                JOIN users u ON f.user_id = u.id
                ORDER BY f.id ASC
                """;

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                int facultyId = resultSet.getInt("faculty_id");
                int userId = resultSet.getInt("user_id");
                String name = resultSet.getString("name");
                String employeeId = resultSet.getString("employee_id");
                String username = resultSet.getString("username");

                List<String> subjects = getSubjectNamesForFaculty(connection, facultyId);

                Map<String, Object> map = new HashMap<>();
                map.put("facultyId", facultyId);
                map.put("userId", userId);
                map.put("id", employeeId);
                map.put("name", name);
                map.put("department", "Department of Computer Science");
                map.put("email", username.contains("@") ? username : employeeId.toLowerCase() + "@college.edu");
                map.put("subjects", subjects);
                map.put("status", "Active");
                list.add(map);
            }
        } catch (SQLException e) {
            System.out.println("Failed to retrieve faculty.");
            e.printStackTrace();
        }
        return list;
    }

    public Faculty findFacultyByUserId(int userId) {
        String sql = """
                SELECT f.id, f.name, f.employee_id, u.username, u.password, u.role
                FROM faculty f
                JOIN users u ON f.user_id = u.id
                WHERE f.user_id = ?
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Faculty(
                            userId,
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("name"),
                            rs.getString("employee_id")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Faculty findFacultyByEmployeeId(String employeeId) {
        String sql = """
                SELECT f.id, f.user_id, f.name, f.employee_id, u.username, u.password, u.role
                FROM faculty f
                JOIN users u ON f.user_id = u.id
                WHERE f.employee_id = ?
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, employeeId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Faculty(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("name"),
                            rs.getString("employee_id")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getFacultyTableIdByUserId(int userId) {
        String sql = "SELECT id FROM faculty WHERE user_id = ?";
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

    public int getFacultyTableIdByEmployeeId(String employeeId) {
        String sql = "SELECT id FROM faculty WHERE employee_id = ?";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, employeeId);
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

    public boolean deleteFacultyByEmployeeId(String employeeId) {
        String sql = """
                DELETE FROM users
                WHERE id = (SELECT user_id FROM faculty WHERE employee_id = ?)
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, employeeId);
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean assignSubject(int facultyId, int subjectId) {
        String sql = """
                INSERT INTO faculty_subjects (faculty_id, subject_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """;
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, facultyId);
            statement.setInt(2, subjectId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeSubjectAssignment(int facultyId, int subjectId) {
        String sql = "DELETE FROM faculty_subjects WHERE faculty_id = ? AND subject_id = ?";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, facultyId);
            statement.setInt(2, subjectId);
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getFacultyCount() {
        String sql = "SELECT count(*) FROM faculty";
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

    private List<String> getSubjectNamesForFaculty(Connection connection, int facultyId) {
        List<String> subjects = new ArrayList<>();
        String sql = """
                SELECT s.name, s.code
                FROM subjects s
                JOIN faculty_subjects fs ON s.id = fs.subject_id
                WHERE fs.faculty_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, facultyId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    subjects.add(rs.getString("name") + " (" + rs.getString("code") + ")");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return subjects;
    }
}
