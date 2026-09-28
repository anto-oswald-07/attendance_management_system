/**
 * CAMS - College Attendance Management System
 * Admin Dashboard Controller (admin/dashboard.js)
 *
 * Populates system statistics and links to backend stats endpoints.
 */

document.addEventListener("DOMContentLoaded", () => {
    loadAdminDashboardStats();
});

/**
 * Loads high-level metrics for the administrator overview
 */
async function loadAdminDashboardStats() {
    console.log("[Admin Dashboard] Initializing system metrics...");

    const studentsEl = document.getElementById("totalStudentsCount");
    const facultyEl = document.getElementById("totalFacultyCount");
    const subjectsEl = document.getElementById("totalSubjectsCount");
    const termEl = document.getElementById("currentAcademicTerm");

    try {
        const stats = await apiRequest(API_ENDPOINTS.ADMIN_STATS);
        if (stats) {
            if (studentsEl && stats.totalStudents !== undefined) studentsEl.textContent = stats.totalStudents;
            if (facultyEl && stats.totalFaculty !== undefined) facultyEl.textContent = stats.totalFaculty;
            if (subjectsEl && stats.totalSubjects !== undefined) subjectsEl.textContent = stats.totalSubjects;
            if (termEl && stats.academicTerm !== undefined) termEl.textContent = stats.academicTerm;
            return;
        }
    } catch (err) {
        console.warn("Failed to load admin stats from backend, using fallback:", err);
    }

    const placeholderStats = {
        totalStudents: 62,
        totalFaculty: 8,
        totalSubjects: 6,
        academicTerm: "Fall 2025 • Semester I"
    };

    if (studentsEl) studentsEl.textContent = placeholderStats.totalStudents;
    if (facultyEl) facultyEl.textContent = placeholderStats.totalFaculty;
    if (subjectsEl) subjectsEl.textContent = placeholderStats.totalSubjects;
    if (termEl) termEl.textContent = placeholderStats.academicTerm;
}
