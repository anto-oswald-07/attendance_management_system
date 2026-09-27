package com.cams.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cams.repository.FacultyRepository;
import com.cams.repository.StudentRepository;
import com.cams.repository.SubjectRepository;

public class AdminService {

    private StudentRepository studentRepository;
    private FacultyRepository facultyRepository;
    private SubjectRepository subjectRepository;

    public AdminService() {
        this.studentRepository = new StudentRepository();
        this.facultyRepository = new FacultyRepository();
        this.subjectRepository = new SubjectRepository();
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", studentRepository.getStudentCount());
        stats.put("totalFaculty", facultyRepository.getFacultyCount());
        stats.put("totalSubjects", subjectRepository.getSubjectCount());
        stats.put("academicTerm", "Fall 2025 • Semester I");
        return stats;
    }

    public List<Map<String, Object>> getAllStudents() {
        return studentRepository.getAllStudentsWithStats();
    }

    public boolean addStudent(String roll, String name, String email, String department, String semester) {
        String username = roll;
        String password = "password123";
        String division = department != null && !department.isEmpty() ? department : "Computer Science";
        return studentRepository.addStudent(username, password, name, roll, division);
    }

    public boolean deleteStudent(String rollNo) {
        return studentRepository.deleteStudentByRollNo(rollNo);
    }

    public List<Map<String, Object>> getAllFaculty() {
        return facultyRepository.getAllFacultyWithDetails();
    }

    public boolean addFaculty(String id, String name, String email, String department, List<String> subjects) {
        String username = id;
        String password = "password123";
        return facultyRepository.addFaculty(username, password, name, id, subjects);
    }

    public boolean deleteFaculty(String employeeId) {
        return facultyRepository.deleteFacultyByEmployeeId(employeeId);
    }

    public List<Map<String, Object>> getAllSubjects() {
        return subjectRepository.getAllSubjectsWithDetails();
    }

    public boolean addSubject(String code, String name, String department, String semester, String faculty, int enrolled) {
        String division = department != null && !department.isEmpty() ? department : "Computer Science";
        return subjectRepository.addSubject(name, code, division, faculty);
    }

    public boolean deleteSubject(String code) {
        return subjectRepository.deleteSubjectByCode(code);
    }

    public boolean assignFacultyToSubject(int facultyId, int subjectId) {
        return facultyRepository.assignSubject(facultyId, subjectId);
    }
}
