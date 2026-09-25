/**
 * CAMS - College Attendance Management System
 * Authentication & Session Management (auth.js)
 *
 * Handles login, role switching, mock session storage, and logout.
 * All API interactions are currently configured with placeholders ready
 * to be wired up to a real backend.
 */

// Key used to store mock session data in localStorage
const AUTH_STORAGE_KEY = "cams_user_session";

/**
 * Default placeholder profiles for each role
 */
const DEFAULT_PROFILES = {
    admin: {
        role: "admin",
        name: "Administrator",
        email: "admin@college.edu",
        id: "ADM-2025-01",
        department: "Academic Affairs"
    },
    faculty: {
        role: "faculty",
        name: "Dr. Rajesh Sharma",
        email: "r.sharma@college.edu",
        id: "FAC-CSE-104",
        department: "Department of Computer Science"
    },
    student: {
        role: "student",
        name: "Rohan Verma",
        email: "rohan.v@college.edu",
        id: "21CSE042",
        department: "Computer Engineering",
        semester: "Semester VI"
    }
};

/**
 * Retrieve current user session or fallback to default role profile
 * @returns {object} User session object
 */
function getCurrentUser() {
    try {
        const stored = localStorage.getItem(AUTH_STORAGE_KEY);
        if (stored) {
            return JSON.parse(stored);
        }
    } catch (e) {
        console.warn("Could not read auth session from storage", e);
    }
    return DEFAULT_PROFILES.admin;
}

/**
 * Set current active session
 * @param {object} user - User session object
 */
function setCurrentUser(user) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
}

/**
 * Handle user login
 * @param {string} identifier - Roll number, Faculty ID, or Admin email
 * @param {string} password - User password
 * @param {string} role - 'admin' | 'faculty' | 'student'
 * @param {string} relativeBasePath - Base path offset for redirect
 * @returns {Promise<boolean>}
 */
async function handleLogin(identifier, password, role = "student", relativeBasePath = "") {
    console.log(`[Auth] Attempting login for Role: ${role}, Identifier: ${identifier}`);

    /*
    // =========================================================================
    // TODO: Connect this function to the backend API
    // =========================================================================
    // const response = await fetch(API_ENDPOINTS.LOGIN, {
    //     method: "POST",
    //     headers: { "Content-Type": "application/json" },
    //     body: JSON.stringify({ identifier, password, role })
    // });
    // const data = await response.json();
    // if (!response.ok) throw new Error(data.message || "Login failed");
    // localStorage.setItem("cams_auth_token", data.token);
    // setCurrentUser(data.user);
    // =========================================================================
    */

    if (!identifier || identifier.trim() === "") {
        throw new Error("Please enter your Roll Number / Institutional ID.");
    }
    if (!password || password.trim() === "") {
        throw new Error("Please enter your password.");
    }

    const profile = { ...DEFAULT_PROFILES[role] };
    if (identifier.trim().length > 2) {
        profile.id = identifier.trim();
    }
    setCurrentUser(profile);

    let redirectUrl = "";
    if (role === "admin") {
        redirectUrl = `${relativeBasePath}admin/dashboard.html`;
    } else if (role === "faculty") {
        redirectUrl = `${relativeBasePath}faculty/dashboard.html`;
    } else {
        redirectUrl = `${relativeBasePath}student/dashboard.html`;
    }

    window.location.href = redirectUrl;
    return true;
}

/**
 * Handle user logout
 * @param {string} redirectPath - Relative path to login.html
 */
function handleLogout(redirectPath = "../login.html") {
    // TODO: Notify backend to invalidate JWT session if token blacklisting is used
    /*
    fetch(API_ENDPOINTS.LOGOUT, { method: "POST" }).catch(console.error);
    */
    localStorage.removeItem(AUTH_STORAGE_KEY);
    localStorage.removeItem("cams_auth_token");
    window.location.href = redirectPath;
}

/**
 * Initialize current user profile into page header elements if present
 */
document.addEventListener("DOMContentLoaded", () => {
    const user = getCurrentUser();

    const userNameEls = document.querySelectorAll("[data-user-name]");
    userNameEls.forEach(el => { el.textContent = user.name; });

    const userDeptEls = document.querySelectorAll("[data-user-dept]");
    userDeptEls.forEach(el => { el.textContent = user.department; });

    const userIdEls = document.querySelectorAll("[data-user-id]");
    userIdEls.forEach(el => { el.textContent = user.id; });

    const logoutBtns = document.querySelectorAll(".logout-btn, [data-action='logout']");
    logoutBtns.forEach(btn => {
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            const target = btn.getAttribute("data-redirect") || "../../pages/login.html";
            handleLogout(target);
        });
    });
});
