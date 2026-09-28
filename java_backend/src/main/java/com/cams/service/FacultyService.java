package com.cams.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cams.model.Subject;
import com.cams.repository.AttendanceRepository;
import com.cams.repository.FacultyRepository;
import com.cams.repository.SubjectRepository;

public class FacultyService {

    private FacultyRepository facultyRepository;
    private SubjectRepository subjectRepository;
    private AttendanceRepository attendanceRepository;

    public FacultyService() {
        this.facultyRepository = new FacultyRepository();
        this.subjectRepository = new SubjectRepository();
        this.attendanceRepository = new AttendanceRepository();
    }

    public Map<String, Object> getFacultyStats(String employeeId) {
        int facultyId = facultyRepository.getFacultyTableIdByEmployeeId(employeeId);
        List<Map<String, Object>> subjects = facultyId > 0
                ? subjectRepository.getSubjectsForFaculty(facultyId)
                : new ArrayList<>();

        LocalDate today = LocalDate.now();
        String dayFormatted = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("assignedCount", subjects.size());
        stats.put("assignedSubjectsCount", subjects.size());
        stats.put("classesToday", Math.min(subjects.size(), 2));
        stats.put("todayClassesCount", Math.min(subjects.size(), 2));
        stats.put("sessionLabel", "Academic Session: 2024–25 (Odd Sem) • Today: " + dayFormatted);
        return stats;
    }

    public List<Map<String, Object>> getAssignedSubjects(String employeeId) {
        int facultyId = facultyRepository.getFacultyTableIdByEmployeeId(employeeId);
        if (facultyId <= 0) {
            return subjectRepository.getAllSubjectsWithDetails();
        }
        return subjectRepository.getSubjectsForFaculty(facultyId);
    }

    public List<Map<String, Object>> getRosterForSubject(String subjectCode) {
        Subject subject = subjectRepository.findSubjectByCode(subjectCode);
        if (subject == null) {
            return new ArrayList<>();
        }
        return attendanceRepository.getRosterWithAttendanceForSubject(subject.getId(), subject.getDivision());
    }

    public void submitAttendance(String subjectCode, LocalDate date, String slot, List<Map<String, String>> roster) throws SQLException {
        Subject subject = subjectRepository.findSubjectByCode(subjectCode);
        if (subject == null) {
            throw new IllegalArgumentException("Invalid subject code: " + subjectCode);
        }
        attendanceRepository.submitAttendance(subject.getId(), date, roster);
    }
}
