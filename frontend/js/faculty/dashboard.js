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

    const countEl = document.getElementById("facultyAssignedCount");
    const todayEl = document.getElementById("facultyClassesToday");
    const sessionEl = document.getElementById("currentFacultySession");

    try {
        const user = typeof getCurrentUser === "function" ? getCurrentUser() : null;
        const facultyParam = user && user.id ? `?facultyId=${encodeURIComponent(user.id)}` : "";
        const stats = await apiRequest(`${API_ENDPOINTS.FACULTY_STATS}${facultyParam}`);
        if (stats) {
            const assigned = stats.assignedSubjectsCount !== undefined ? stats.assignedSubjectsCount : stats.assignedCount;
            const today = stats.todayClassesCount !== undefined ? stats.todayClassesCount : stats.classesToday;

            if (countEl && assigned !== undefined) countEl.textContent = assigned;
            if (todayEl && today !== undefined) todayEl.textContent = today;
            if (sessionEl && stats.sessionLabel) sessionEl.textContent = stats.sessionLabel;
            return;
        }
    } catch (err) {
        console.warn("Failed to load faculty stats from backend, using defaults:", err);
    }

    const facultySessionInfo = {
        assignedCount: 3,
        classesToday: 2,
        sessionLabel: "Academic Session: 2024–25 (Odd Sem) • Today: Monday, 24 Feb 2025"
    };

    if (countEl) countEl.textContent = facultySessionInfo.assignedCount;
    if (todayEl) todayEl.textContent = facultySessionInfo.classesToday;
    if (sessionEl) sessionEl.textContent = facultySessionInfo.sessionLabel;
}
