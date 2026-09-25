/**
 * CAMS - College Attendance Management System
 * Student Dashboard Controller (student/dashboard.js)
 *
 * Renders cumulative attendance meter, subject progress cards, and recent history.
 */

const studentProfileData = {
    name: "Rohan Verma",
    roll: "21CSE042",
    department: "Computer Engineering",
    semester: "Semester VI",
    overallPct: 86.4,
    totalConducted: 142,
    totalAttended: 123,
    totalMissed: 19,
    shortageRiskCount: 0,
    subjects: [
        {
            code: "CS301",
            title: "Data Structures",
            faculty: "Dr. Rajesh Sharma",
            attended: 38,
            total: 42,
            pct: 90.5
        },
        {
            code: "CS402",
            title: "Java Programming",
            faculty: "Dr. Rajesh Sharma",
            attended: 34,
            total: 40,
            pct: 85.0
        },
        {
            code: "CS601",
            title: "Database Management",
            faculty: "Dr. Rajesh Sharma",
            attended: 30,
            total: 34,
            pct: 88.2
        },
        {
            code: "CS501",
            title: "Operating Systems",
            faculty: "Prof. Sunita Rao",
            attended: 21,
            total: 26,
            pct: 80.8
        }
    ],
    recentLogs: [
        { date: "24 Feb 2025", subject: "CS601 - Database Management", slot: "08:30 AM - 09:30 AM", faculty: "Dr. Rajesh Sharma", status: "P" },
        { date: "22 Feb 2025", subject: "CS301 - Data Structures", slot: "10:00 AM - 11:00 AM", faculty: "Dr. Rajesh Sharma", status: "P" },
        { date: "21 Feb 2025", subject: "CS402 - Java Programming", slot: "02:00 PM - 03:00 PM", faculty: "Dr. Rajesh Sharma", status: "A" },
        { date: "20 Feb 2025", subject: "CS501 - Operating Systems", slot: "11:30 AM - 12:30 PM", faculty: "Prof. Sunita Rao", status: "P" },
        { date: "19 Feb 2025", subject: "CS301 - Data Structures", slot: "10:00 AM - 11:00 AM", faculty: "Dr. Rajesh Sharma", status: "P" }
    ]
};

document.addEventListener("DOMContentLoaded", () => {
    loadStudentDashboard();
});

async function loadStudentDashboard() {
    console.log("[Student Dashboard] Loading student progress and compliance records...");

    /*
    // =========================================================================
    // TODO: Connect this function to the backend API
    // Example:
    // try {
    //     const data = await apiRequest(API_ENDPOINTS.STUDENT_DASHBOARD);
    //     // Render with data received from backend API
    // } catch (err) {
    //     console.error("Failed to load student dashboard:", err);
    // }
    // =========================================================================
    */

    renderComplianceSummary(studentProfileData);
    renderSubjectCards(studentProfileData.subjects);
    renderRecentLogs(studentProfileData.recentLogs);
}

function renderComplianceSummary(data) {
    const headingEl = document.getElementById("studentHeadingName");
    const overallEl = document.getElementById("overallAttendancePct");
    const conductedEl = document.getElementById("statTotalConducted");
    const attendedEl = document.getElementById("statTotalAttended");
    const missedEl = document.getElementById("statTotalMissed");
    const shortageEl = document.getElementById("statShortageCount");

    if (headingEl) headingEl.textContent = data.name.split(" ")[0];
    if (overallEl) {
        overallEl.textContent = `${data.overallPct.toFixed(1)}%`;
        if (data.overallPct < 75) overallEl.className = "compliance-percentage red";
        else if (data.overallPct < 80) overallEl.className = "compliance-percentage amber";
    }
    if (conductedEl) conductedEl.textContent = data.totalConducted;
    if (attendedEl) attendedEl.textContent = data.totalAttended;
    if (missedEl) missedEl.textContent = data.totalMissed;
    if (shortageEl) shortageEl.textContent = `${data.shortageRiskCount} Courses`;
}

function renderSubjectCards(subjects) {
    const container = document.getElementById("studentSubjectCards");
    if (!container) return;
    container.innerHTML = "";

    subjects.forEach(sub => {
        let colorClass = "";
        let standingTag = "Eligible";
        let badgeType = "badge-green";

        if (sub.pct < 75) {
            colorClass = "red";
            standingTag = "Shortage Alert";
            badgeType = "badge-red";
        } else if (sub.pct < 80) {
            colorClass = "amber";
            standingTag = "Warning Zone";
            badgeType = "badge-amber";
        }

        const card = document.createElement("div");
        card.className = "student-subject-card";
        card.innerHTML = `
            <div>
                <div class="student-card-header">
                    <span class="student-card-code">${sub.code}</span>
                    <span class="badge ${badgeType}">${standingTag}</span>
                </div>
                <h3 class="student-card-title">${sub.title}</h3>
                <div class="student-card-faculty">${sub.faculty}</div>

                <div class="attendance-progress-wrap">
                    <div class="progress-header">
                        <span>Attendance Rate</span>
                        <strong style="color: ${sub.pct < 75 ? 'var(--danger)' : 'var(--text-primary)'};">${sub.pct.toFixed(1)}%</strong>
                    </div>
                    <div class="progress-bar-bg">
                        <div class="progress-bar-fill ${colorClass}" style="width: ${sub.pct}%;"></div>
                    </div>
                </div>
            </div>

            <div class="attendance-stats-summary">
                <span>Attended: <strong>${sub.attended} / ${sub.total}</strong></span>
                <span class="text-xs text-muted">Req: &ge;75%</span>
            </div>
        `;
        container.appendChild(card);
    });
}

function renderRecentLogs(logs) {
    const tbody = document.getElementById("studentRecentActivityBody");
    if (!tbody) return;
    tbody.innerHTML = "";

    logs.forEach(log => {
        const isPresent = log.status === "P";
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${log.date}</strong></td>
            <td>${log.subject}</td>
            <td>${log.slot}</td>
            <td class="text-muted">${log.faculty}</td>
            <td>
                <span class="badge ${isPresent ? 'badge-green' : 'badge-red'}">
                    <span class="status-dot ${isPresent ? 'green' : 'red'}"></span>
                    ${isPresent ? 'Present' : 'Absent'}
                </span>
            </td>
        `;
        tbody.appendChild(tr);
    });
}
