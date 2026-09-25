/**
 * CAMS - College Attendance Management System
 * Faculty Dashboard Controller (faculty/dashboard.js)
 *
 * Populates professor lecture schedules, allocated subjects, and daily stats.
 */

document.addEventListener("DOMContentLoaded", () => {
    loadFacultyStats();
});

/**
 * Loads today's schedule and metrics for the logged-in professor
 */
async function loadFacultyStats() {
    console.log("[Faculty Dashboard] Loading teaching schedule and active modules...");

    /*
    // =========================================================================
    // TODO: Connect this function to the backend API
    // Example:
    // try {
    //     const stats = await apiRequest(API_ENDPOINTS.FACULTY_STATS);
    //     document.getElementById("facultyAssignedCount").textContent = stats.assignedSubjectsCount;
    //     document.getElementById("facultyClassesToday").textContent = stats.todayClassesCount;
    //     document.getElementById("currentFacultySession").textContent = stats.sessionLabel;
    // } catch (err) {
    //     console.error("Failed to load faculty stats from backend:", err);
    // }
    // =========================================================================
    */

    const facultySessionInfo = {
        assignedCount: 3,
        classesToday: 2,
        sessionLabel: "Academic Session: 2024–25 (Odd Sem) • Today: Monday, 24 Feb 2025"
    };

    const countEl = document.getElementById("facultyAssignedCount");
    const todayEl = document.getElementById("facultyClassesToday");
    const sessionEl = document.getElementById("currentFacultySession");

    if (countEl) countEl.textContent = facultySessionInfo.assignedCount;
    if (todayEl) todayEl.textContent = facultySessionInfo.classesToday;
    if (sessionEl) sessionEl.textContent = facultySessionInfo.sessionLabel;
}
