package com.cams.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.cams.model.Faculty;
import com.cams.model.Student;
import com.cams.model.User;
import com.cams.service.AdminService;
import com.cams.service.AuthService;
import com.cams.service.FacultyService;
import com.cams.service.StudentService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class HttpApiServer {

    private final int port;
    private HttpServer server;

    private final AuthService authService;
    private final AdminService adminService;
    private final FacultyService facultyService;
    private final StudentService studentService;

    public HttpApiServer(int port) {
        this.port = port;
        this.authService = new AuthService();
        this.adminService = new AdminService();
        this.facultyService = new FacultyService();
        this.studentService = new StudentService();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/api", this::handleRequest);

        server.start();
        System.out.println("CAMS HTTP Backend Server started on http://localhost:" + port + "/api");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleRequest(HttpExchange exchange) {
        setCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendResponse(exchange, 204, "");
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        Map<String, String> params = parseQueryParams(uri.getQuery());

        try {
            if (path.equals("/api/auth/login") && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else if (path.equals("/api/auth/logout") && "POST".equalsIgnoreCase(method)) {
                handleLogout(exchange);
            } else if (path.equals("/api/auth/verify") && "GET".equalsIgnoreCase(method)) {
                handleVerifyToken(exchange);
            } else if (path.equals("/api/admin/stats") && "GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, adminService.getDashboardStats());
            } else if (path.startsWith("/api/admin/students")) {
                handleAdminStudents(exchange, method, path, params);
            } else if (path.startsWith("/api/admin/faculty")) {
                handleAdminFaculty(exchange, method, path, params);
            } else if (path.startsWith("/api/admin/subjects")) {
                handleAdminSubjects(exchange, method, path, params);
            } else if (path.equals("/api/faculty/stats") && "GET".equalsIgnoreCase(method)) {
                handleFacultyStats(exchange, params);
            } else if (path.equals("/api/faculty/subjects") && "GET".equalsIgnoreCase(method)) {
                handleFacultySubjects(exchange, params);
            } else if (path.equals("/api/faculty/roster") && "GET".equalsIgnoreCase(method)) {
                handleFacultyRoster(exchange, params);
            } else if (path.equals("/api/faculty/attendance/submit") && "POST".equalsIgnoreCase(method)) {
                handleFacultyAttendanceSubmit(exchange);
            } else if (path.equals("/api/faculty/attendance/history") && "GET".equalsIgnoreCase(method)) {
                handleFacultyRoster(exchange, params);
            } else if (path.equals("/api/student/dashboard") && "GET".equalsIgnoreCase(method)) {
                handleStudentDashboard(exchange, params);
            } else if (path.equals("/api/student/attendance") && "GET".equalsIgnoreCase(method)) {
                handleStudentAttendance(exchange, params);
            } else {
                sendError(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error processing " + method + " " + path + ": " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        String identifier = (String) req.get("identifier");
        if (identifier == null) {
            identifier = (String) req.get("username");
        }
        String password = (String) req.get("password");

        if (identifier == null || identifier.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            sendError(exchange, 400, "Missing credentials.");
            return;
        }

        User user = authService.authenticate(identifier, password);
        if (user == null) {
            sendError(exchange, 401, "Invalid username, roll number, or password.");
            return;
        }

        String token = authService.createSession(user);
        Map<String, Object> profile = authService.buildUserProfile(user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("user", profile);

        sendJson(exchange, 200, resp);
    }

    private void handleLogout(HttpExchange exchange) {
        String token = extractBearerToken(exchange);
        authService.invalidateSession(token);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Logged out successfully.");
        sendJson(exchange, 200, resp);
    }

    private void handleVerifyToken(HttpExchange exchange) {
        User user = getUserFromToken(exchange);
        if (user == null) {
            sendError(exchange, 401, "Session expired or invalid token.");
            return;
        }
        sendJson(exchange, 200, authService.buildUserProfile(user));
    }

    private void handleAdminStudents(HttpExchange exchange, String method, String path, Map<String, String> params) throws IOException {
        if ("GET".equalsIgnoreCase(method)) {
            sendJson(exchange, 200, adminService.getAllStudents());
        } else if ("POST".equalsIgnoreCase(method)) {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);
            String roll = (String) req.get("roll");
            String name = (String) req.get("name");
            String email = (String) req.get("email");
            String dept = (String) req.get("department");
            String sem = (String) req.get("semester");

            if (roll == null || name == null || roll.trim().isEmpty() || name.trim().isEmpty()) {
                sendError(exchange, 400, "Student roll and name are required.");
                return;
            }

            boolean success = adminService.addStudent(roll.trim(), name.trim(), email, dept, sem);
            if (success) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Student created successfully.");
                resp.put("id", roll.trim());
                sendJson(exchange, 201, resp);
            } else {
                sendError(exchange, 400, "Failed to create student. Roll number or username might already exist.");
            }
        } else if ("DELETE".equalsIgnoreCase(method)) {
            String roll = extractIdFromPath(path, "/api/admin/students");
            if (roll == null || roll.isEmpty()) {
                roll = params.get("id");
            }
            if (roll == null || roll.isEmpty()) {
                sendError(exchange, 400, "Missing student identifier.");
                return;
            }

            boolean deleted = adminService.deleteStudent(roll);
            if (deleted) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Student record removed.");
                sendJson(exchange, 200, resp);
            } else {
                sendError(exchange, 404, "Student not found.");
            }
        }
    }

    private void handleAdminFaculty(HttpExchange exchange, String method, String path, Map<String, String> params) throws IOException {
        if ("GET".equalsIgnoreCase(method)) {
            sendJson(exchange, 200, adminService.getAllFaculty());
        } else if ("POST".equalsIgnoreCase(method)) {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);
            String id = (String) req.get("id");
            String name = (String) req.get("name");
            String email = (String) req.get("email");
            String dept = (String) req.get("department");

            List<String> subjects = new ArrayList<>();
            Object subs = req.get("subjects");
            if (subs instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) subjects.add(item.toString());
                }
            }

            if (id == null || name == null || id.trim().isEmpty() || name.trim().isEmpty()) {
                sendError(exchange, 400, "Faculty ID and name are required.");
                return;
            }

            boolean success = adminService.addFaculty(id.trim(), name.trim(), email, dept, subjects);
            if (success) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Faculty created successfully.");
                resp.put("id", id.trim());
                sendJson(exchange, 201, resp);
            } else {
                sendError(exchange, 400, "Failed to create faculty. Employee ID or username might already exist.");
            }
        } else if ("DELETE".equalsIgnoreCase(method)) {
            String id = extractIdFromPath(path, "/api/admin/faculty");
            if (id == null || id.isEmpty()) {
                id = params.get("id");
            }
            if (id == null || id.isEmpty()) {
                sendError(exchange, 400, "Missing faculty identifier.");
                return;
            }

            boolean deleted = adminService.deleteFaculty(id);
            if (deleted) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Faculty member removed.");
                sendJson(exchange, 200, resp);
            } else {
                sendError(exchange, 404, "Faculty not found.");
            }
        }
    }

    private void handleAdminSubjects(HttpExchange exchange, String method, String path, Map<String, String> params) throws IOException {
        if ("GET".equalsIgnoreCase(method)) {
            sendJson(exchange, 200, adminService.getAllSubjects());
        } else if ("POST".equalsIgnoreCase(method)) {
            String body = readBody(exchange);
            Map<String, Object> req = JsonUtil.parseObject(body);
            String code = (String) req.get("code");
            String name = (String) req.get("name");
            String dept = (String) req.get("department");
            String sem = (String) req.get("semester");
            String faculty = (String) req.get("faculty");
            int enrolled = 60;
            if (req.get("enrolled") instanceof Number n) {
                enrolled = n.intValue();
            }

            if (code == null || name == null || code.trim().isEmpty() || name.trim().isEmpty()) {
                sendError(exchange, 400, "Subject code and name are required.");
                return;
            }

            boolean success = adminService.addSubject(code.trim(), name.trim(), dept, sem, faculty, enrolled);
            if (success) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Subject created successfully.");
                resp.put("code", code.trim());
                sendJson(exchange, 201, resp);
            } else {
                sendError(exchange, 400, "Failed to create subject. Course code might already exist.");
            }
        } else if ("DELETE".equalsIgnoreCase(method)) {
            String code = extractIdFromPath(path, "/api/admin/subjects");
            if (code == null || code.isEmpty()) {
                code = params.get("code");
            }
            if (code == null || code.isEmpty()) {
                sendError(exchange, 400, "Missing subject course code.");
                return;
            }

            boolean deleted = adminService.deleteSubject(code);
            if (deleted) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("message", "Subject removed from curriculum.");
                sendJson(exchange, 200, resp);
            } else {
                sendError(exchange, 404, "Subject not found.");
            }
        }
    }

    private void handleFacultyStats(HttpExchange exchange, Map<String, String> params) {
        String empId = params.get("facultyId");
        if (empId == null) {
            User user = getUserFromToken(exchange);
            if (user instanceof Faculty f) {
                empId = f.getEmployeeId();
            }
        }
        sendJson(exchange, 200, facultyService.getFacultyStats(empId));
    }

    private void handleFacultySubjects(HttpExchange exchange, Map<String, String> params) {
        String empId = params.get("facultyId");
        if (empId == null) {
            User user = getUserFromToken(exchange);
            if (user instanceof Faculty f) {
                empId = f.getEmployeeId();
            }
        }
        sendJson(exchange, 200, facultyService.getAssignedSubjects(empId));
    }

    private void handleFacultyRoster(HttpExchange exchange, Map<String, String> params) {
        String subject = params.get("subject");
        if (subject == null || subject.trim().isEmpty()) {
            subject = "CS301";
        }
        sendJson(exchange, 200, facultyService.getRosterForSubject(subject));
    }

    private void handleFacultyAttendanceSubmit(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.parseObject(body);
        String subjectCode = (String) req.get("subjectCode");
        String dateStr = (String) req.get("date");
        String slot = (String) req.get("slot");

        if (subjectCode == null || dateStr == null) {
            sendError(exchange, 400, "Subject code and date are required.");
            return;
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid date format. Expected YYYY-MM-DD.");
            return;
        }

        List<Map<String, String>> roster = new ArrayList<>();
        Object rosterRaw = req.get("roster");
        if (rosterRaw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("roll", String.valueOf(m.get("roll")));
                    entry.put("status", String.valueOf(m.get("status")));
                    entry.put("remark", m.get("remark") != null ? String.valueOf(m.get("remark")) : "");
                    roster.add(entry);
                }
            }
        }

        try {
            facultyService.submitAttendance(subjectCode, date, slot, roster);
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Attendance successfully synced with institutional ledger!");
            sendJson(exchange, 200, resp);
        } catch (SQLException e) {
            sendError(exchange, 409, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleStudentDashboard(HttpExchange exchange, Map<String, String> params) {
        String roll = params.get("roll");
        if (roll == null) {
            roll = params.get("id");
        }
        if (roll == null) {
            User user = getUserFromToken(exchange);
            if (user instanceof Student s) {
                roll = s.getRollNo();
            }
        }
        sendJson(exchange, 200, studentService.getStudentDashboard(roll));
    }

    private void handleStudentAttendance(HttpExchange exchange, Map<String, String> params) {
        String roll = params.get("roll");
        if (roll == null) {
            roll = params.get("id");
        }
        if (roll == null) {
            User user = getUserFromToken(exchange);
            if (user instanceof Student s) {
                roll = s.getRollNo();
            }
        }
        sendJson(exchange, 200, studentService.getStudentAttendanceDetails(roll));
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) {
        String json = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("error", message);
        sendJson(exchange, statusCode, err);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseText) {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(statusCode, bytes.length > 0 ? bytes.length : -1);
            if (bytes.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractBearerToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private User getUserFromToken(HttpExchange exchange) {
        String token = extractBearerToken(exchange);
        if (token != null) {
            return authService.getUserByToken(token);
        }
        return null;
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            } else if (kv.length == 1) {
                map.put(kv[0].trim(), "");
            }
        }
        return map;
    }

    private String extractIdFromPath(String fullPath, String basePath) {
        if (fullPath.startsWith(basePath)) {
            String remainder = fullPath.substring(basePath.length());
            if (remainder.startsWith("/")) {
                remainder = remainder.substring(1);
            }
            if (!remainder.isEmpty()) {
                return remainder;
            }
        }
        return null;
    }

    private void setCorsHeaders(HttpExchange exchange) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        String allowedOrigin = determineAllowedOrigin(requestOrigin);

        if (allowedOrigin != null && !allowedOrigin.isEmpty()) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigin);
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
            exchange.getResponseHeaders().set("Vary", "Origin");
        } else {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }

    private String determineAllowedOrigin(String requestOrigin) {
        if (requestOrigin == null || requestOrigin.trim().isEmpty()) {
            return null;
        }
        requestOrigin = requestOrigin.trim();

        // 1. Check explicit FRONTEND_URL or CAMS_FRONTEND_URL or ALLOWED_ORIGINS
        String envFrontends = System.getenv("FRONTEND_URL");
        if (envFrontends == null) envFrontends = System.getenv("CAMS_FRONTEND_URL");
        if (envFrontends == null) envFrontends = System.getenv("ALLOWED_ORIGINS");

        if (envFrontends != null && !envFrontends.trim().isEmpty()) {
            String[] allowedList = envFrontends.split(",");
            for (String allowed : allowedList) {
                allowed = allowed.trim();
                if (allowed.equalsIgnoreCase(requestOrigin) ||
                    (allowed.endsWith("/") && allowed.substring(0, allowed.length() - 1).equalsIgnoreCase(requestOrigin)) ||
                    (requestOrigin.endsWith("/") && requestOrigin.substring(0, requestOrigin.length() - 1).equalsIgnoreCase(allowed))) {
                    return requestOrigin;
                }
            }
        }

        // 2. Allow Vercel production & preview deployments (*.vercel.app)
        if (requestOrigin.matches("^https://[a-zA-Z0-9_.-]+\\.vercel\\.app$")) {
            return requestOrigin;
        }

        // 3. Allow local development (localhost / 127.0.0.1)
        if (requestOrigin.matches("^http://(localhost|127\\.0\\.0\\.1)(:\\d+)?$")) {
            return requestOrigin;
        }

        // 4. If FRONTEND_URL was configured, default to its first origin
        if (envFrontends != null && !envFrontends.trim().isEmpty()) {
            return envFrontends.split(",")[0].trim();
        }

        return requestOrigin;
    }
}
