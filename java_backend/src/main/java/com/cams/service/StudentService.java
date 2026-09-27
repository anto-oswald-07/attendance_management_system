package com.cams.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cams.model.Student;
import com.cams.repository.AttendanceRepository;
import com.cams.repository.StudentRepository;

public class StudentService {

    private StudentRepository studentRepository;
    private AttendanceRepository attendanceRepository;

    public StudentService() {
        this.studentRepository = new StudentRepository();
        this.attendanceRepository = new AttendanceRepository();
    }

    public Map<String, Object> getStudentDashboard(String rollNo) {
        Student student = studentRepository.findStudentByRollNo(rollNo);
        if (student == null) {
            List<Map<String, Object>> all = studentRepository.getAllStudentsWithStats();
            if (!all.isEmpty()) {
                rollNo = (String) all.get(0).get("rollNo");
                student = studentRepository.findStudentByRollNo(rollNo);
            }
        }

        if (student == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("name", "Student");
            empty.put("roll", rollNo != null ? rollNo : "N/A");
            empty.put("department", "Computer Engineering");
            empty.put("semester", "Semester VI");
            empty.put("overallPct", 100.0);
            empty.put("totalConducted", 0);
            empty.put("totalAttended", 0);
            empty.put("totalMissed", 0);
            empty.put("shortageRiskCount", 0);
            empty.put("subjects", new ArrayList<>());
            empty.put("recentLogs", new ArrayList<>());
            return empty;
        }

        int studentTableId = studentRepository.getStudentTableIdByRollNo(student.getRollNo());
        Map<String, Object> stats = attendanceRepository.getStudentOverallStats(studentTableId, student.getDivision());
        List<Map<String, Object>> history = attendanceRepository.getStudentAttendanceHistory(studentTableId);

        List<Map<String, Object>> recentLogs = history.size() > 5 ? history.subList(0, 5) : history;

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("name", student.getName());
        dashboard.put("roll", student.getRollNo());
        dashboard.put("department", student.getDivision());
        dashboard.put("semester", "Semester VI");
        dashboard.put("overallPct", stats.get("overallPct"));
        dashboard.put("totalConducted", stats.get("totalConducted"));
        dashboard.put("totalAttended", stats.get("totalAttended"));
        dashboard.put("totalMissed", stats.get("totalMissed"));
        dashboard.put("shortageRiskCount", stats.get("shortageRiskCount"));
        dashboard.put("subjects", stats.get("subjects"));
        dashboard.put("recentLogs", recentLogs);
        return dashboard;
    }

    public Map<String, Object> getStudentAttendanceDetails(String rollNo) {
        Student student = studentRepository.findStudentByRollNo(rollNo);
        if (student == null) {
            List<Map<String, Object>> all = studentRepository.getAllStudentsWithStats();
            if (!all.isEmpty()) {
                rollNo = (String) all.get(0).get("rollNo");
                student = studentRepository.findStudentByRollNo(rollNo);
            }
        }

        Map<String, Object> response = new HashMap<>();
        if (student == null) {
            response.put("summary", new ArrayList<>());
            response.put("history", new ArrayList<>());
            return response;
        }

        int studentTableId = studentRepository.getStudentTableIdByRollNo(student.getRollNo());
        List<Map<String, Object>> summary = attendanceRepository.getStudentSubjectsSummary(studentTableId, student.getDivision());
        List<Map<String, Object>> history = attendanceRepository.getStudentAttendanceHistory(studentTableId);

        response.put("summary", summary);
        response.put("history", history);
        return response;
    }
}
