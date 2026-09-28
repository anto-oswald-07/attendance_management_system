/**
 * CAMS - College Attendance Management System
 * Global API & Application Configuration
 *
 * This file centralizes all backend endpoints and environment settings.
 * When integrating with a real backend (Node.js, Python/Django, Spring Boot, PHP, etc.),
 * update the API_BASE_URL and relevant endpoint paths below.
 */

// Railway Production Backend Base URL
// When deployed, your Railway Java backend endpoint is configured here
const RAILWAY_BACKEND_URL = "https://attendance-management-system-production.up.railway.app/api";

// Determine whether running locally or on production deployment (Vercel)
const isLocalhost = typeof window !== "undefined" && (
    window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1" ||
    window.location.protocol === "file:"
);

// Base API URL:
// 1. window.CAMS_API_URL if explicitly injected
// 2. http://localhost:5000/api if running on localhost
// 3. RAILWAY_BACKEND_URL for production (Vercel)
const API_BASE_URL = (typeof window !== "undefined" && window.CAMS_API_URL)
    ? window.CAMS_API_URL
    : (isLocalhost ? "http://localhost:5000/api" : RAILWAY_BACKEND_URL);

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
 * Standard fetch helper with authorization headers
 * @param {string} endpoint - Target URL
 * @param {object} options - Fetch options (method, headers, body)
 * @returns {Promise<any>} - Parsed JSON response
 */
async function apiRequest(endpoint, options = {}) {
    const token = typeof localStorage !== "undefined" ? localStorage.getItem("cams_auth_token") : null;
    const defaultHeaders = {
        "Content-Type": "application/json",
        ...(token ? { "Authorization": `Bearer ${token}` } : {})
    };

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...(options.headers || {})
        }
    };

    try {
        console.log(`[API Request] -> ${config.method || "GET"} ${endpoint}`);
        const response = await fetch(endpoint, config);

        if (response.status === 204) {
            return { success: true };
        }

        const contentType = response.headers.get("content-type");
        const isJson = contentType && contentType.includes("application/json");
        const data = isJson ? await response.json() : await response.text();

        if (!response.ok) {
            const errorMsg = (data && typeof data === "object" && (data.error || data.message))
                ? (data.error || data.message)
                : `HTTP ${response.status}: ${response.statusText}`;
            throw new Error(errorMsg);
        }

        return data;
    } catch (error) {
        console.error(`[API Error] Failed to fetch ${endpoint}:`, error);
        throw error;
    }
}
