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

    /*
    // =========================================================================
    // TODO: Connect this function to the backend API
    // Example:
    // try {
    //     const stats = await apiRequest(API_ENDPOINTS.ADMIN_STATS);
    //     document.getElementById("totalStudentsCount").textContent = stats.totalStudents;
    //     document.getElementById("totalFacultyCount").textContent = stats.totalFaculty;
    //     document.getElementById("totalSubjectsCount").textContent = stats.totalSubjects;
    //     document.getElementById("currentAcademicTerm").textContent = stats.academicTerm;
    // } catch (err) {
    //     console.error("Failed to load admin stats from backend:", err);
    // }
    // =========================================================================
    */

    const placeholderStats = {
        totalStudents: 62,
        totalFaculty: 8,
        totalSubjects: 6,
        academicTerm: "Fall 2025 • Semester I"
    };

    const studentsEl = document.getElementById("totalStudentsCount");
    const facultyEl = document.getElementById("totalFacultyCount");
    const subjectsEl = document.getElementById("totalSubjectsCount");
    const termEl = document.getElementById("currentAcademicTerm");

    if (studentsEl) studentsEl.textContent = placeholderStats.totalStudents;
    if (facultyEl) facultyEl.textContent = placeholderStats.totalFaculty;
    if (subjectsEl) subjectsEl.textContent = placeholderStats.totalSubjects;
    if (termEl) termEl.textContent = placeholderStats.academicTerm;
}
