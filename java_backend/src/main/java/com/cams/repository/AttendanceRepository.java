package com.cams.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cams.database.Database;

public class AttendanceRepository {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public boolean hasAttendanceForSubjectAndDate(int subjectId, LocalDate date) {
        String sql = "SELECT 1 FROM attendance WHERE subject_id = ? AND date = ? LIMIT 1";
        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, subjectId);
            statement.setDate(2, Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean submitAttendance(int subjectId, LocalDate date, List<Map<String, String>> roster) throws SQLException {
        if (hasAttendanceForSubjectAndDate(subjectId, date)) {
            throw new SQLException("Attendance already recorded for this subject on date: " + date);
        }

        String insertSql = """
                INSERT INTO attendance (student_id, subject_id, date, status)
                VALUES (?, ?, ?, ?)
                """;

        String findStudentSql = "SELECT id FROM students WHERE roll_no = ?";

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (
                PreparedStatement findStmt = connection.prepareStatement(findStudentSql);
                PreparedStatement insertStmt = connection.prepareStatement(insertSql)
            ) {
                for (Map<String, String> entry : roster) {
                    String roll = entry.get("roll");
                    String rawStatus = entry.get("status");
                    if (roll == null || rawStatus == null) continue;

                    findStmt.setString(1, roll.trim());
                    int studentId = -1;
                    try (ResultSet rs = findStmt.executeQuery()) {
                        if (rs.next()) {
                            studentId = rs.getInt("id");
                        }
                    }

                    if (studentId <= 0) continue;

                    String dbStatus = (rawStatus.equalsIgnoreCase("P") || rawStatus.equalsIgnoreCase("PRESENT") || rawStatus.equalsIgnoreCase("L")) ? "PRESENT" : "ABSENT";

                    insertStmt.setInt(1, studentId);
                    insertStmt.setInt(2, subjectId);
                    insertStmt.setDate(3, Date.valueOf(date));
                    insertStmt.setString(4, dbStatus);
                    insertStmt.addBatch();
                }

                insertStmt.executeBatch();
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Map<String, Object>> getStudentAttendanceHistory(int studentId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT a.date, a.status, s.code, s.name as subject_name,
                       COALESCE(f.name, 'Faculty') as faculty_name
                FROM attendance a
                JOIN subjects s ON a.subject_id = s.id
                LEFT JOIN faculty_subjects fs ON s.id = fs.subject_id
                LEFT JOIN faculty f ON fs.faculty_id = f.id
                WHERE a.student_id = ?
                ORDER BY a.date DESC
                """;

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = rs.getDate("date").toLocalDate();
                    String dbStatus = rs.getString("status");
                    String statusShort = "PRESENT".equalsIgnoreCase(dbStatus) ? "P" : "A";

                    Map<String, Object> map = new HashMap<>();
                    map.put("date", d.format(DATE_FORMATTER));
                    map.put("code", rs.getString("code"));
                    map.put("title", rs.getString("subject_name"));
                    map.put("subject", rs.getString("code") + " - " + rs.getString("subject_name"));
                    map.put("slot", "10:00 AM - 11:00 AM");
                    map.put("faculty", rs.getString("faculty_name"));
                    map.put("status", statusShort);
                    map.put("remark", "Regular Lecture");
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> getStudentSubjectsSummary(int studentId, String division) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT s.id, s.code, s.name,
                       COALESCE(f.name, 'Faculty') as faculty_name,
                       count(a.id) as total_conducted,
                       count(CASE WHEN a.status = 'PRESENT' THEN 1 END) as total_attended
                FROM subjects s
                LEFT JOIN faculty_subjects fs ON s.id = fs.subject_id
                LEFT JOIN faculty f ON fs.faculty_id = f.id
                LEFT JOIN attendance a ON s.id = a.subject_id AND a.student_id = ?
                WHERE s.division = ?
                GROUP BY s.id, s.code, s.name, f.name
                ORDER BY s.code ASC
                """;

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, studentId);
            statement.setString(2, division);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("total_conducted");
                    int attended = rs.getInt("total_attended");
                    int missed = total - attended;
                    double pct = total > 0 ? Math.round(((double) attended / total * 100.0) * 10.0) / 10.0 : 100.0;

                    Map<String, Object> map = new HashMap<>();
                    map.put("code", rs.getString("code"));
                    map.put("title", rs.getString("name"));
                    map.put("faculty", rs.getString("faculty_name"));
                    map.put("total", total);
                    map.put("attended", attended);
                    map.put("missed", missed);
                    map.put("pct", pct);
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Map<String, Object> getStudentOverallStats(int studentId, String division) {
        List<Map<String, Object>> subjects = getStudentSubjectsSummary(studentId, division);
        int totalConducted = 0;
        int totalAttended = 0;
        int totalMissed = 0;
        int shortageRiskCount = 0;

        for (Map<String, Object> sub : subjects) {
            int tot = (Integer) sub.get("total");
            int att = (Integer) sub.get("attended");
            int mis = (Integer) sub.get("missed");
            double pct = (Double) sub.get("pct");

            totalConducted += tot;
            totalAttended += att;
            totalMissed += mis;

            if (pct < 75.0 && tot > 0) {
                shortageRiskCount++;
            }
        }

        double overallPct = totalConducted > 0
                ? Math.round(((double) totalAttended / totalConducted * 100.0) * 10.0) / 10.0
                : 100.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("overallPct", overallPct);
        stats.put("totalConducted", totalConducted);
        stats.put("totalAttended", totalAttended);
        stats.put("totalMissed", totalMissed);
        stats.put("shortageRiskCount", shortageRiskCount);
        stats.put("subjects", subjects);
        return stats;
    }

    public List<Map<String, Object>> getRosterWithAttendanceForSubject(int subjectId, String division) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT st.id, st.roll_no, st.name,
                       count(a.id) as total_subject_classes,
                       count(CASE WHEN a.status = 'PRESENT' THEN 1 END) as attended_subject_classes
                FROM students st
                LEFT JOIN attendance a ON st.id = a.student_id AND a.subject_id = ?
                WHERE st.division = ?
                GROUP BY st.id, st.roll_no, st.name
                ORDER BY st.roll_no ASC
                """;

        try (
            Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, subjectId);
            statement.setString(2, division);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("total_subject_classes");
                    int attended = rs.getInt("attended_subject_classes");
                    double pct = total > 0 ? Math.round(((double) attended / total * 100.0) * 10.0) / 10.0 : 100.0;

                    Map<String, Object> map = new HashMap<>();
                    map.put("roll", rs.getString("roll_no"));
                    map.put("name", rs.getString("name"));
                    map.put("attendance", pct);
                    map.put("standing", pct < 75.0 ? "Shortage Alert" : "Eligible");
                    map.put("status", "P");
                    map.put("remark", "");
                    map.put("cumulativePct", pct);
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
