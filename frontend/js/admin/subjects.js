/**
 * CAMS - College Attendance Management System
 * Admin Subjects Controller (admin/subjects.js)
 *
 * Manages curriculum modules, instructor allocations, and enrollment quotas.
 */

let subjectsData = [];

let activeDeleteSubjectCode = null;

document.addEventListener("DOMContentLoaded", () => {
    initSubjectsView();
});

function initSubjectsView() {
    renderSubjectsTable(subjectsData);

    const searchInput = document.getElementById("subjectSearchInput");
    const deptFilter = document.getElementById("subjectDeptFilter");
    const semFilter = document.getElementById("subjectSemFilter");

    function applyFilters() {
        const query = searchInput.value.toLowerCase().trim();
        const selectedDept = deptFilter.value;
        const selectedSem = semFilter.value;

        const filtered = subjectsData.filter(sub => {
            const matchesQuery = sub.name.toLowerCase().includes(query) ||
                                 sub.code.toLowerCase().includes(query) ||
                                 sub.faculty.toLowerCase().includes(query);
            const matchesDept = !selectedDept || sub.department === selectedDept;
            const matchesSem = !selectedSem || sub.semester === selectedSem;

            return matchesQuery && matchesDept && matchesSem;
        });

        renderSubjectsTable(filtered);
    }

    searchInput.addEventListener("input", applyFilters);
    deptFilter.addEventListener("change", applyFilters);
    semFilter.addEventListener("change", applyFilters);

    setupSubjectModals();
}

function renderSubjectsTable(list) {
    const tbody = document.getElementById("subjectTableBody");
    const countEl = document.getElementById("subjectResultCount");
    if (countEl) countEl.textContent = list.length;

    if (!tbody) return;
    tbody.innerHTML = "";

    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    No subjects found matching the filter query.
                </td>
            </tr>
        `;
        return;
    }

    list.forEach(sub => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${sub.code}</strong></td>
            <td><strong>${sub.name}</strong></td>
            <td><span class="dept-pill">${sub.department}</span></td>
            <td><span class="sem-pill">${sub.semester}</span></td>
            <td>${sub.faculty}</td>
            <td><span class="badge badge-gray">${sub.enrolled} students</span></td>
            <td style="text-align: right;">
                <div class="table-actions" style="justify-content: flex-end;">
                    <button class="icon-action-btn edit-subject-btn" data-code="${sub.code}" title="Edit Subject">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                        Edit
                    </button>
                    <button class="icon-action-btn delete delete-subject-btn" data-code="${sub.code}" data-name="${sub.name}" title="Delete Subject">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="3 6 5 6 21 6"></polyline>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });

    document.querySelectorAll(".edit-subject-btn").forEach(btn => {
        btn.addEventListener("click", () => openEditSubjectModal(btn.dataset.code));
    });
    document.querySelectorAll(".delete-subject-btn").forEach(btn => {
        btn.addEventListener("click", () => openDeleteSubjectModal(btn.dataset.code, btn.dataset.name));
    });
}

function setupSubjectModals() {
    const modal = document.getElementById("subjectModal");
    const openBtn = document.getElementById("openAddSubjectModal");
    const closeBtn = document.getElementById("closeSubjectModal");
    const cancelBtn = document.getElementById("cancelSubjectModal");
    const form = document.getElementById("subjectForm");

    const deleteModal = document.getElementById("confirmDeleteSubjectModal");
    const closeDeleteBtn = document.getElementById("closeDeleteSubjectModal");
    const cancelDeleteBtn = document.getElementById("cancelDeleteSubjectBtn");
    const confirmDeleteBtn = document.getElementById("confirmDeleteSubjectBtn");

    openBtn.addEventListener("click", () => {
        document.getElementById("modalSubjectTitle").textContent = "Add Course Module";
        form.reset();
        modal.classList.add("active");
    });

    const closeModal = () => modal.classList.remove("active");
    closeBtn.addEventListener("click", closeModal);
    cancelBtn.addEventListener("click", closeModal);

    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const code = document.getElementById("formSubjectCode").value.trim().toUpperCase();
        const name = document.getElementById("formSubjectName").value.trim();
        const dept = document.getElementById("formSubjectDept").value;
        const sem = document.getElementById("formSubjectSem").value;
        const faculty = document.getElementById("formSubjectFaculty").value;
        const enrolled = parseInt(document.getElementById("formSubjectEnrollment").value, 10) || 60;

        /*
        // =====================================================================
        // TODO: Connect this function to the backend API
        // const payload = { code, name, department: dept, semester: sem, faculty, enrolled };
        // await apiRequest(API_ENDPOINTS.SUBJECTS, { method: "POST", body: JSON.stringify(payload) });
        // =====================================================================
        */

        subjectsData.unshift({ code, name, department: dept, semester: sem, faculty, enrolled });
        renderSubjectsTable(subjectsData);
        closeModal();
        showToast(`Subject ${code}: ${name} created successfully!`, "success");
    });

    const hideDeleteModal = () => deleteModal.classList.remove("active");
    closeDeleteBtn.addEventListener("click", hideDeleteModal);
    cancelDeleteBtn.addEventListener("click", hideDeleteModal);

    confirmDeleteBtn.addEventListener("click", () => {
        if (activeDeleteSubjectCode) {
            /*
            // =================================================================
            // TODO: Delete subject module in backend API
            // await apiRequest(`${API_ENDPOINTS.SUBJECTS}/${activeDeleteSubjectCode}`, { method: "DELETE" });
            // =================================================================
            */
            subjectsData = subjectsData.filter(s => s.code !== activeDeleteSubjectCode);
            renderSubjectsTable(subjectsData);
            hideDeleteModal();
            showToast("Subject removed from curriculum.", "info");
        }
    });
}

function openEditSubjectModal(code) {
    const sub = subjectsData.find(s => s.code === code);
    if (!sub) return;

    document.getElementById("modalSubjectTitle").textContent = `Edit Subject: ${sub.code}`;
    document.getElementById("formSubjectCode").value = sub.code;
    document.getElementById("formSubjectName").value = sub.name;
    document.getElementById("formSubjectDept").value = sub.department;
    document.getElementById("formSubjectSem").value = sub.semester;
    document.getElementById("formSubjectFaculty").value = sub.faculty;
    document.getElementById("formSubjectEnrollment").value = sub.enrolled;

    document.getElementById("subjectModal").classList.add("active");
}

function openDeleteSubjectModal(code, name) {
    activeDeleteSubjectCode = code;
    document.getElementById("deleteTargetSubjectName").textContent = `${name} (${code})`;
    document.getElementById("confirmDeleteSubjectModal").classList.add("active");
}

function showToast(message, type = "info") {
    const container = document.getElementById("toastContainer");
    if (!container) return;
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3200);
}
