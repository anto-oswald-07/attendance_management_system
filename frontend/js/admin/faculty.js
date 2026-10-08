/**
 * CAMS - College Attendance Management System
 * Admin Faculty Controller (admin/faculty.js)
 *
 * Handles staff registry rendering, search, filtering, and modal flows.
 */

// Demo placeholder faculty list
let facultyData = [];

let activeDeleteFacultyId = null;

document.addEventListener("DOMContentLoaded", () => {
    initFacultyView();
});

function initFacultyView() {
    renderFacultyTable(facultyData);

    const searchInput = document.getElementById("facultySearchInput");
    const deptFilter = document.getElementById("facultyDeptFilter");

    function applyFilters() {
        const query = searchInput.value.toLowerCase().trim();
        const selectedDept = deptFilter.value;

        const filtered = facultyData.filter(fac => {
            const matchesQuery = fac.name.toLowerCase().includes(query) ||
                                 fac.id.toLowerCase().includes(query) ||
                                 fac.email.toLowerCase().includes(query);
            const matchesDept = !selectedDept || fac.department === selectedDept;

            return matchesQuery && matchesDept;
        });

        renderFacultyTable(filtered);
    }

    searchInput.addEventListener("input", applyFilters);
    deptFilter.addEventListener("change", applyFilters);

    setupFacultyModals();
    loadFacultyFromBackend();
}

async function loadFacultyFromBackend() {
    try {
        const data = await apiRequest(API_ENDPOINTS.FACULTY);
        if (Array.isArray(data) && data.length > 0) {
            facultyData = data;
            renderFacultyTable(facultyData);
        }
    } catch (err) {
        console.warn("Could not load faculty from backend, using default list:", err);
    }
}

function renderFacultyTable(list) {
    const tbody = document.getElementById("facultyTableBody");
    const countEl = document.getElementById("facultyResultCount");
    if (countEl) countEl.textContent = list.length;

    if (!tbody) return;
    tbody.innerHTML = "";

    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    No faculty records found matching filters.
                </td>
            </tr>
        `;
        return;
    }

    list.forEach(fac => {
        const subjectTags = fac.subjects.map(s => `<span class="badge badge-blue" style="margin: 2px;">${s}</span>`).join(" ");

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${fac.id}</strong></td>
            <td><strong>${fac.name}</strong></td>
            <td><span class="dept-pill">${fac.department}</span></td>
            <td class="text-muted" style="font-size: 0.825rem;">${fac.email}</td>
            <td><div style="display: flex; flex-wrap: wrap; max-width: 320px;">${subjectTags}</div></td>
            <td>
                <span class="badge badge-green">
                    <span class="status-dot green"></span> Active
                </span>
            </td>
            <td style="text-align: right;">
                <div class="table-actions" style="justify-content: flex-end;">
                    <button class="icon-action-btn edit-faculty-btn" data-id="${fac.id}" title="Edit Faculty">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                        Edit
                    </button>
                    <button class="icon-action-btn delete delete-faculty-btn" data-id="${fac.id}" data-name="${fac.name}" title="Remove Faculty">
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

    document.querySelectorAll(".edit-faculty-btn").forEach(btn => {
        btn.addEventListener("click", () => openEditFacultyModal(btn.dataset.id));
    });
    document.querySelectorAll(".delete-faculty-btn").forEach(btn => {
        btn.addEventListener("click", () => openDeleteFacultyModal(btn.dataset.id, btn.dataset.name));
    });
}

function setupFacultyModals() {
    const modal = document.getElementById("facultyModal");
    const openBtn = document.getElementById("openAddFacultyModal");
    const closeBtn = document.getElementById("closeFacultyModal");
    const cancelBtn = document.getElementById("cancelFacultyModal");
    const form = document.getElementById("facultyForm");

    const deleteModal = document.getElementById("confirmDeleteFacultyModal");
    const closeDeleteBtn = document.getElementById("closeDeleteFacultyModal");
    const cancelDeleteBtn = document.getElementById("cancelDeleteFacultyBtn");
    const confirmDeleteBtn = document.getElementById("confirmDeleteFacultyBtn");

    openBtn.addEventListener("click", () => {
        document.getElementById("modalFacultyTitle").textContent = "Add New Faculty Member";
        form.reset();
        modal.classList.add("active");
    });

    const closeModal = () => modal.classList.remove("active");
    closeBtn.addEventListener("click", closeModal);
    cancelBtn.addEventListener("click", closeModal);

    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const id = document.getElementById("formFacultyId").value.trim();
        const name = document.getElementById("formFacultyName").value.trim();
        const email = document.getElementById("formFacultyEmail").value.trim();
        const dept = document.getElementById("formFacultyDept").value;
        const subRaw = document.getElementById("formFacultySubjects").value.trim();
        const subjects = subRaw ? subRaw.split(",").map(s => s.trim()) : ["General Elective"];

        const payload = { id, name, email, department: dept, subjects };
        apiRequest(API_ENDPOINTS.FACULTY, {
            method: "POST",
            body: JSON.stringify(payload)
        }).catch(err => console.warn("Notice: Faculty save API call:", err));

        facultyData.unshift({
            id,
            name,
            department: dept,
            email,
            subjects,
            status: "Active"
        });

        renderFacultyTable(facultyData);
        closeModal();
        showToast(`Faculty member ${name} added successfully!`, "success");
    });

    const hideDeleteModal = () => deleteModal.classList.remove("active");
    closeDeleteBtn.addEventListener("click", hideDeleteModal);
    cancelDeleteBtn.addEventListener("click", hideDeleteModal);

    confirmDeleteBtn.addEventListener("click", () => {
        if (activeDeleteFacultyId) {
            apiRequest(
                `${API_ENDPOINTS.FACULTY}/${encodeURIComponent(activeDeleteFacultyId)}`,
                { method: "DELETE" }
            ).catch(err => console.warn("Notice: Faculty delete API call:", err));

            facultyData = facultyData.filter(f => f.id !== activeDeleteFacultyId);
            renderFacultyTable(facultyData);
            hideDeleteModal();
            showToast("Faculty member removed.", "info");
        }
    });
}

function openEditFacultyModal(id) {
    const fac = facultyData.find(f => f.id === id);
    if (!fac) return;

    document.getElementById("modalFacultyTitle").textContent = `Edit Faculty: ${fac.name}`;
    document.getElementById("formFacultyId").value = fac.id;
    document.getElementById("formFacultyName").value = fac.name;
    document.getElementById("formFacultyEmail").value = fac.email;
    document.getElementById("formFacultyDept").value = fac.department;
    document.getElementById("formFacultySubjects").value = fac.subjects.join(", ");

    document.getElementById("facultyModal").classList.add("active");
}

function openDeleteFacultyModal(id, name) {
    activeDeleteFacultyId = id;
    document.getElementById("deleteTargetFacultyName").textContent = `${name} (${id})`;
    document.getElementById("confirmDeleteFacultyModal").classList.add("active");
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
