/**
 * CAMS - College Attendance Management System
 * Global API & Application Configuration
 *
 * This file centralizes all backend endpoints and environment settings.
 * When integrating with a real backend (Node.js, Python/Django, Spring Boot, PHP, etc.),
 * update the API_BASE_URL and relevant endpoint paths below.
 */

// Base API URL - Replace with your actual backend server host
const API_BASE_URL = "http://localhost:5000/api";

// Endpoints configuration map
const API_ENDPOINTS = {
    // Auth endpoints
    LOGIN: `${API_BASE_URL}/auth/login`,
    LOGOUT: `${API_BASE_URL}/auth/logout`,
    VERIFY_TOKEN: `${API_BASE_URL}/auth/verify`,

    // Admin endpoints
    ADMIN_STATS: `${API_BASE_URL}/admin/stats`,
    STUDENTS: `${API_BASE_URL}/admin/students`,
    FACULTY: `${API_BASE_URL}/admin/faculty`,
    SUBJECTS: `${API_BASE_URL}/admin/subjects`,

    // Faculty endpoints
    FACULTY_STATS: `${API_BASE_URL}/faculty/stats`,
    FACULTY_SUBJECTS: `${API_BASE_URL}/faculty/subjects`,
    FACULTY_ROSTER: `${API_BASE_URL}/faculty/roster`,
    SUBMIT_ATTENDANCE: `${API_BASE_URL}/faculty/attendance/submit`,
    ATTENDANCE_HISTORY: `${API_BASE_URL}/faculty/attendance/history`,

    // Student endpoints
    STUDENT_DASHBOARD: `${API_BASE_URL}/student/dashboard`,
    STUDENT_ATTENDANCE: `${API_BASE_URL}/student/attendance`
};

/**
 * Standard fetch helper with authorization headers placeholder
 * @param {string} endpoint - Target URL
 * @param {object} options - Fetch options (method, headers, body)
 * @returns {Promise<any>} - Parsed JSON response
 */
async function apiRequest(endpoint, options = {}) {
    const defaultHeaders = {
        "Content-Type": "application/json",
        // TODO: Attach real JWT or Bearer token once backend authentication is ready
        // "Authorization": `Bearer ${localStorage.getItem("cams_auth_token") || ""}`
    };

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers
        }
    };

    try {
        console.log(`[API Request] -> ${config.method || "GET"} ${endpoint}`);
        // NOTE: Backend is not yet connected. When ready, uncomment the fetch call below:
        /*
        const response = await fetch(endpoint, config);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json();
        */

        // Returning null as placeholder
        return null;
    } catch (error) {
        console.error(`[API Error] Failed to fetch ${endpoint}:`, error);
        throw error;
    }
}
