/**
 * CAMS - College Attendance Management System
 * Student Attendance Controller (student/attendance.js)
 *
 * Handles detailed subject-wise calculations, safety margin estimators,
 * date-wise session ledger filtering, and print functionality.
 */

const studentSubjectsSummary = [
    {
        code: "CS301",
        title: "Data Structures",
        faculty: "Dr. Rajesh Sharma",
        total: 42,
        attended: 38,
        missed: 4,
        pct: 90.5
    },
    {
        code: "CS402",
        title: "Java Programming",
        faculty: "Dr. Rajesh Sharma",
        total: 40,
        attended: 34,
        missed: 6,
        pct: 85.0
    },
    {
        code: "CS601",
        title: "Database Management",
        faculty: "Dr. Rajesh Sharma",
        total: 34,
        attended: 30,
        missed: 4,
        pct: 88.2
    },
    {
        code: "CS501",
        title: "Operating Systems",
        faculty: "Prof. Sunita Rao",
        total: 26,
        attended: 21,
        missed: 5,
        pct: 80.8
    }
];

const attendanceHistoryRecords = [
    { date: "24 Feb 2025", code: "CS601", title: "Database Management", slot: "08:30 AM - 09:30 AM", faculty: "Dr. Rajesh Sharma", status: "P", remark: "Indexed Queries Lab" },
    { date: "22 Feb 2025", code: "CS301", title: "Data Structures", slot: "10:00 AM - 11:00 AM", faculty: "Dr. Rajesh Sharma", status: "P", remark: "Binary Search Trees" },
    { date: "21 Feb 2025", code: "CS402", title: "Java Programming", slot: "02:00 PM - 03:00 PM", faculty: "Dr. Rajesh Sharma", status: "A", remark: "Unexcused" },
    { date: "20 Feb 2025", code: "CS501", title: "Operating Systems", slot: "11:30 AM - 12:30 PM", faculty: "Prof. Sunita Rao", status: "P", remark: "Process Scheduling" },
    { date: "19 Feb 2025", code: "CS301", title: "Data Structures", slot: "10:00 AM - 11:00 AM", faculty: "Dr. Rajesh Sharma", status: "P", remark: "AVL Tree Rotations" },
    { date: "18 Feb 2025", code: "CS402", title: "Java Programming", slot: "02:00 PM - 03:00 PM", faculty: "Dr. Rajesh Sharma", status: "P", remark: "Multithreading Basics" },
    { date: "17 Feb 2025", code: "CS601", title: "Database Management", slot: "08:30 AM - 09:30 AM", faculty: "Dr. Rajesh Sharma", status: "P", remark: "Normalization 3NF" },
    { date: "15 Feb 2025", code: "CS501", title: "Operating Systems", slot: "11:30 AM - 12:30 PM", faculty: "Prof. Sunita Rao", status: "A", remark: "Sick leave acknowledged" }
];

document.addEventListener("DOMContentLoaded", () => {
    initStudentAttendance();
});

async function initStudentAttendance() {
    /*
    // =========================================================================
    // TODO: Connect this function to the backend API
    // Example:
    // try {
    //     const data = await apiRequest(API_ENDPOINTS.STUDENT_ATTENDANCE);
    //     // Render with real backend student records
    // } catch (err) {
    //     console.error("Failed to load attendance logs:", err);
    // }
    // =========================================================================
    */

    renderSummaryTable(studentSubjectsSummary);
    renderHistoryTable(attendanceHistoryRecords);

    const subjectFilter = document.getElementById("historySubjectFilter");
    const statusFilter = document.getElementById("historyStatusFilter");

    function applyHistoryFilters() {
        const sub = subjectFilter.value;
        const stat = statusFilter.value;

        const filtered = attendanceHistoryRecords.filter(item => {
            const matchesSub = !sub || item.code === sub;
            const matchesStat = !stat || item.status === stat;
            return matchesSub && matchesStat;
        });

        renderHistoryTable(filtered);
    }

    if (subjectFilter) subjectFilter.addEventListener("change", applyHistoryFilters);
    if (statusFilter) statusFilter.addEventListener("change", applyHistoryFilters);

    const printBtn = document.getElementById("btnPrintReport");
    if (printBtn) {
        printBtn.addEventListener("click", () => window.print());
    }
}

function calculateSafetyMargin(attended, total) {
    const minPct = 0.75;
    const canMiss = Math.floor((attended - (minPct * total)) / minPct);

    if (canMiss > 0) {
        return `<span style="color: var(--success); font-weight: 600;">Can safely miss ${canMiss} ${canMiss === 1 ? 'class' : 'classes'}</span>`;
    } else if (canMiss === 0) {
        return `<span style="color: var(--warning); font-weight: 600;">Borderline &bull; Cannot miss next class</span>`;
    } else {
        const mustAttend = Math.ceil(((minPct * total) - attended) / (1 - minPct));
        return `<span style="color: var(--danger); font-weight: 600;">Must attend next ${mustAttend} classes</span>`;
    }
}

function renderSummaryTable(subjects) {
    const tbody = document.getElementById("studentSummaryTableBody");
    if (!tbody) return;
    tbody.innerHTML = "";

    subjects.forEach(sub => {
        const isLow = sub.pct < 75;
        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td>
                <strong>${sub.code}</strong> &bull; ${sub.title}
            </td>
            <td class="text-muted">${sub.faculty}</td>
            <td>${sub.total}</td>
            <td style="color: var(--success); font-weight: 600;">${sub.attended}</td>
            <td style="color: var(--danger); font-weight: 600;">${sub.missed}</td>
            <td>
                <strong style="color: ${isLow ? 'var(--danger)' : 'var(--text-primary)'};">
                    ${sub.pct.toFixed(1)}%
                </strong>
            </td>
            <td>
                <span class="badge ${isLow ? 'badge-red' : 'badge-green'}">
                    <span class="status-dot ${isLow ? 'red' : 'green'}"></span>
                    ${isLow ? 'Shortage Alert' : 'In Compliance'}
                </span>
            </td>
            <td>${calculateSafetyMargin(sub.attended, sub.total)}</td>
        `;
        tbody.appendChild(tr);
    });
}

function renderHistoryTable(records) {
    const tbody = document.getElementById("studentHistoryTableBody");
    const countEl = document.getElementById("historyRecordCount");
    if (countEl) countEl.textContent = records.length;
    if (!tbody) return;

    tbody.innerHTML = "";

    if (records.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    No attendance records match your filter criteria.
                </td>
            </tr>
        `;
        return;
    }

    records.forEach(rec => {
        const isP = rec.status === "P";
        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td><strong>${rec.date}</strong></td>
            <td><strong>${rec.code}</strong> &bull; ${rec.title}</td>
            <td>${rec.slot}</td>
            <td class="text-muted">${rec.faculty}</td>
            <td>
                <span class="history-status-badge ${isP ? 'present' : 'absent'}">
                    ${isP ? '&check; Present' : '&times; Absent'}
                </span>
            </td>
            <td class="text-muted" style="font-size: 0.825rem;">${rec.remark || '&mdash;'}</td>
        `;
        tbody.appendChild(tr);
    });
}
