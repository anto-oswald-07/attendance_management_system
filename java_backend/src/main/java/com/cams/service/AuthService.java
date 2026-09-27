package com.cams.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cams.model.Faculty;
import com.cams.model.Student;
import com.cams.model.User;
import com.cams.repository.UserRepository;
import com.cams.security.PasswordHasher;

public class AuthService {

    private UserRepository userRepository;
    private static final Map<String, User> sessions = new ConcurrentHashMap<>();

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    public User authenticate(String identifier, String password) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        if (password == null || password.trim().isEmpty()) {
            return null;
        }

        User user = userRepository.findUserByIdentifier(identifier.trim());
        if (user == null) {
            return null;
        }

        boolean valid = PasswordHasher.verifyPassword(password, user.getPassword());
        if (!valid) {
            return null;
        }

        return user;
    }

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public User getUserByToken(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    public void invalidateSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public Map<String, Object> buildUserProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("role", user.getRole());

        if (user instanceof Student student) {
            profile.put("name", student.getName());
            profile.put("rollNo", student.getRollNo());
            profile.put("division", student.getDivision());
            profile.put("department", student.getDivision());
            profile.put("email", user.getUsername().contains("@") ? user.getUsername() : student.getRollNo().toLowerCase() + "@college.edu");
        } else if (user instanceof Faculty faculty) {
            profile.put("name", faculty.getName());
            profile.put("employeeId", faculty.getEmployeeId());
            profile.put("department", "Department of Computer Science");
            profile.put("email", user.getUsername().contains("@") ? user.getUsername() : faculty.getEmployeeId().toLowerCase() + "@college.edu");
        } else {
            profile.put("name", "Administrator");
            profile.put("department", "Academic Affairs");
            profile.put("email", user.getUsername().contains("@") ? user.getUsername() : "admin@college.edu");
        }

        return profile;
    }
}