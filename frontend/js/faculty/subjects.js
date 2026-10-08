/**
 * CAMS - College Attendance Management System
 * Faculty Subjects Controller (faculty/subjects.js)
 *
 * Manages assigned subjects display, class roster inspection, and backend placeholders.
 */

let facultySubjects = [


];

const demoRosters = {

};

document.addEventListener("DOMContentLoaded", () => {
    initFacultySubjects();
});

function initFacultySubjects() {
    renderSubjectsTable(facultySubjects);

    const searchInput = document.getElementById("facultySubjectSearch");
    if (searchInput) {
        searchInput.addEventListener("input", (e) => {
            const query = e.target.value.toLowerCase().trim();
            const filtered = facultySubjects.filter(sub =>
                sub.title.toLowerCase().includes(query) ||
                sub.code.toLowerCase().includes(query) ||
                sub.section.toLowerCase().includes(query)
            );
            renderSubjectsTable(filtered);
        });
    }

    setupRosterModal();
}

function renderSubjectsTable(list) {
    const tbody = document.getElementById("facultySubjectTableBody");
    const countEl = document.getElementById("assignedSubjectsCount");
    if (countEl) countEl.textContent = list.length;
    if (!tbody) return;

    tbody.innerHTML = "";

    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    No courses match your filter criteria.
                </td>
            </tr>
        `;
        return;
    }

    list.forEach(sub => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${sub.code}</strong></td>
            <td><strong>${sub.title}</strong></td>
            <td><span class="dept-pill">${sub.section}</span></td>
            <td><span class="sem-pill">${sub.semester}</span></td>
            <td><span class="badge badge-gray">${sub.studentsCount} Students</span></td>
            <td><strong>${sub.avgAttendance.toFixed(1)}%</strong></td>
            <td>
                <span class="badge badge-green">
                    <span class="status-dot green"></span> ${sub.status}
                </span>
            </td>
            <td style="text-align: right;">
                <div class="table-actions" style="justify-content: flex-end;">
                    <button class="icon-action-btn view-roster-btn" data-code="${sub.code}" data-title="${sub.title}">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                        </svg>
                        Roster
                    </button>
                    <a href="attendance.html?subject=${sub.code}" class="btn btn-primary btn-sm" style="padding: 4px 10px; font-size: 0.8rem;">
                        Take Roll Call &rarr;
                    </a>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });

    document.querySelectorAll(".view-roster-btn").forEach(btn => {
        btn.addEventListener("click", () => openRosterModal(btn.dataset.code, btn.dataset.title));
    });
}

function setupRosterModal() {
    const modal = document.getElementById("rosterModal");
    const closeBtn = document.getElementById("closeRosterModal");
    const closeBtn2 = document.getElementById("closeRosterModalBtn");

    const closeModal = () => modal.classList.remove("active");
    if (closeBtn) closeBtn.addEventListener("click", closeModal);
    if (closeBtn2) closeBtn2.addEventListener("click", closeModal);
}

function openRosterModal(code, title) {
    const modal = document.getElementById("rosterModal");
    const titleEl = document.getElementById("rosterModalTitle");
    const tbody = document.getElementById("rosterTableBody");

    titleEl.textContent = `${code} &bull; ${title} Roster`;
    tbody.innerHTML = "";

    /*
    // =========================================================================
    // TODO: Connect this function to the backend API to fetch real course roster
    // const rosterData = await apiRequest(`${API_ENDPOINTS.FACULTY_ROSTER}?subject=${code}`);
    // =========================================================================
    */

    const students = demoRosters[code] || demoRosters["CS301"];
    students.forEach(st => {
        const isLow = st.attendance < 75;
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${st.roll}</strong></td>
            <td>${st.name}</td>
            <td>${st.attendance.toFixed(1)}%</td>
            <td>
                <span class="badge ${isLow ? 'badge-red' : 'badge-green'}">
                    ${st.standing}
                </span>
            </td>
        `;
        tbody.appendChild(tr);
    });

    modal.classList.add("active");
}
