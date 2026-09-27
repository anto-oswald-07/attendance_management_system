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
import com.cams.model.Subject;

public class SubjectRepository {

    public boolean addSubject(String name, String code, String division, String facultyIdentifier) {
        String insertSubjectSql = """
                INSERT INTO subjects (name, code, division)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int subjectId = -1;
                try (PreparedStatement statement = connection.prepareStatement(insertSubjectSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, name);
                    statement.setString(2, code);
                    statement.setString(3, division);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            subjectId = keys.getInt(1);
                        }
                    }
                }

                if (subjectId > 0 && facultyIdentifier != null && !facultyIdentifier.trim().isEmpty()) {
                    String findFacultySql = """
                            SELECT id FROM faculty
                            WHERE employee_id = ? OR name = ?
                            """;
                    int facultyId = -1;
                    try (PreparedStatement fStmt = connection.prepareStatement(findFacultySql)) {
                        fStmt.setString(1, facultyIdentifier.trim());
                        fStmt.setString(2, facultyIdentifier.trim());
                        try (ResultSet rs = fStmt.executeQuery()) {
                            if (rs.next()) {
                                facultyId = rs.getInt("id");
                            }
                        }
                    }

                    if (facultyId > 0) {
                        String assignSql = """
                                INSERT INTO faculty_subjects (faculty_id, subject_id)
                                VALUES (?, ?)
                                ON CONFLICT DO NOTHING
                                """;
                        try (PreparedStatement aStmt = connection.prepareStatement(assignSql)) {
                            aStmt.setInt(1, facultyId);
                            aStmt.setInt(2, subjectId);
                            aStmt.executeUpdate();
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

    public List<Map<String, Object>> getAllSubjectsWithDetails() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT s.id, s.name, s.code, s.division,
                       f.id as faculty_id, f.name as faculty_name, f.employee_id,
                       (SELECT count(*) FROM students st WHERE st.division = s.division) as enrolled_count
                FROM subjects s
                LEFT JOIN faculty_subjects fs ON s.id = fs.subject_id
                LEFT JOIN faculty f ON fs.faculty_id = f.id
                ORDER BY s.id ASC
                """;

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery()
        ) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("code", rs.getString("code"));
                map.put("name", rs.getString("name"));
                map.put("department", rs.getString("division"));
                map.put("semester", "Semester 6");
                String fName = rs.getString("faculty_name");
                map.put("faculty", fName != null ? fName : "Unassigned");
                map.put("facultyId", rs.getInt("faculty_id"));
                map.put("facultyEmployeeId", rs.getString("employee_id"));
                map.put("enrolled", rs.getInt("enrolled_count"));
                list.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Subject findSubjectById(int id) {
        String sql = "SELECT id, name, code, division FROM subjects WHERE id = ?";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Subject(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("code"),
                            rs.getString("division")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Subject findSubjectByCode(String code) {
        String sql = "SELECT id, name, code, division FROM subjects WHERE code = ?";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Subject(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("code"),
                            rs.getString("division")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteSubjectByCode(String code) {
        String sql = "DELETE FROM subjects WHERE code = ?";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, code);
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getSubjectsForFaculty(int facultyId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT s.id, s.name, s.code, s.division,
                       (SELECT count(*) FROM students st WHERE st.division = s.division) as enrolled_count
                FROM subjects s
                JOIN faculty_subjects fs ON s.id = fs.subject_id
                WHERE fs.faculty_id = ?
                ORDER BY s.id ASC
                """;

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, facultyId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int subId = rs.getInt("id");
                    double avgAtt = getAverageAttendanceForSubject(connection, subId);

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", subId);
                    map.put("code", rs.getString("code"));
                    map.put("title", rs.getString("name"));
                    map.put("section", rs.getString("division"));
                    map.put("semester", "Semester 6");
                    map.put("studentsCount", rs.getInt("enrolled_count"));
                    map.put("avgAttendance", avgAtt);
                    map.put("status", "Active");
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Subject> getSubjectsByDivision(String division) {
        List<Subject> list = new ArrayList<>();
        String sql = "SELECT id, name, code, division FROM subjects WHERE division = ? ORDER BY id ASC";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, division);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    list.add(new Subject(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("code"),
                            rs.getString("division")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getSubjectCount() {
        String sql = "SELECT count(*) FROM subjects";
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

    private double getAverageAttendanceForSubject(Connection connection, int subjectId) {
        String sql = """
                SELECT count(*) as total, count(CASE WHEN status = 'PRESENT' THEN 1 END) as attended
                FROM attendance
                WHERE subject_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, subjectId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int attended = rs.getInt("attended");
                    if (total == 0) return 85.0;
                    return Math.round(((double) attended / total * 100.0) * 10.0) / 10.0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 85.0;
    }
}
