/**
 * CAMS - College Attendance Management System
 * Admin Students Controller (admin/students.js)
 *
 * Manages student data presentation, client-side filtering, and modal flows.
 * Real backend API integrations should be attached where marked with TODO.
 */

let studentsData = [
    {
        id: "21CSE001",
        name: "Aarav Sharma",
        department: "Computer Science",
        semester: "Semester 6",
        email: "aarav.sharma@college.edu",
        attendancePct: 92.5,
        status: "Good Standing"
    },
    {
        id: "21CSE014",
        name: "Diya Deshmukh",
        department: "Computer Science",
        semester: "Semester 6",
        email: "diya.d@college.edu",
        attendancePct: 88.0,
        status: "Good Standing"
    },
    {
        id: "21ECE022",
        name: "Karan Johar",
        department: "Electronics & Comm",
        semester: "Semester 4",
        email: "karan.j@college.edu",
        attendancePct: 71.4,
        status: "Shortage Alert"
    },
    {
        id: "21IT009",
        name: "Meera Nair",
        department: "Information Tech",
        semester: "Semester 6",
        email: "meera.n@college.edu",
        attendancePct: 95.0,
        status: "Good Standing"
    },
    {
        id: "21ME031",
        name: "Nikhil Joshi",
        department: "Mechanical Eng",
        semester: "Semester 4",
        email: "nikhil.j@college.edu",
        attendancePct: 68.2,
        status: "Shortage Alert"
    },
    {
        id: "21CSE042",
        name: "Rohan Verma",
        department: "Computer Science",
        semester: "Semester 6",
        email: "rohan.v@college.edu",
        attendancePct: 86.4,
        status: "Good Standing"
    }
];

let activeDeleteStudentId = null;
let editingStudentId = null;

document.addEventListener("DOMContentLoaded", () => {
    initStudentsView();
});

function initStudentsView() {
    renderStudentsTable(studentsData);

    const searchInput = document.getElementById("studentSearchInput");
    const deptFilter = document.getElementById("deptFilter");
    const semFilter = document.getElementById("semFilter");

    function applyFilters() {
        const query = searchInput ? searchInput.value.toLowerCase().trim() : "";
        const selectedDept = deptFilter ? deptFilter.value : "";
        const selectedSem = semFilter ? semFilter.value : "";

        const filtered = studentsData.filter(student => {
            const matchesQuery =
                student.name.toLowerCase().includes(query) ||
                student.id.toLowerCase().includes(query) ||
                student.email.toLowerCase().includes(query);

            const matchesDept = !selectedDept || student.department === selectedDept;
            const matchesSem = !selectedSem || student.semester === selectedSem;

            return matchesQuery && matchesDept && matchesSem;
        });

        renderStudentsTable(filtered);
    }

    if (searchInput) {
        searchInput.addEventListener("input", applyFilters);
    }

    if (deptFilter) {
        deptFilter.addEventListener("change", applyFilters);
    }

    if (semFilter) {
        semFilter.addEventListener("change", applyFilters);
    }

    window.applyStudentFilters = applyFilters;

    setupStudentModals();
    loadStudentsFromBackend();
}

async function loadStudentsFromBackend() {
    try {
        const data = await apiRequest(API_ENDPOINTS.STUDENTS);
        if (Array.isArray(data) && data.length > 0) {
            studentsData = data;
            if (window.applyStudentFilters) {
                window.applyStudentFilters();
            } else {
                renderStudentsTable(studentsData);
            }
        }
    } catch (err) {
        console.warn("Could not load students from backend, using default list:", err);
    }
}

function renderStudentsTable(list) {
    const tbody = document.getElementById("studentTableBody");
    const countEl = document.getElementById("studentResultCount");

    if (countEl) {
        countEl.textContent = list.length;
    }

    if (!tbody) return;

    tbody.innerHTML = "";

    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 2rem; color: var(--text-muted);">
                    No student records found matching the specified filters.
                </td>
            </tr>
        `;
        return;
    }

    list.forEach(student => {
        const isLow = student.attendancePct < 75;
        const badgeClass = isLow ? "badge-red" : "badge-green";
        const dotClass = isLow ? "red" : "green";

        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td><strong>${student.id}</strong></td>
            <td>${student.name}</td>
            <td><span class="dept-pill">${student.department}</span></td>
            <td><span class="sem-pill">${student.semester}</span></td>
            <td class="text-muted" style="font-size: 0.8rem;">${student.email}</td>
            <td><strong>${student.attendancePct.toFixed(1)}%</strong></td>
            <td>
                <span class="badge ${badgeClass}">
                    <span class="status-dot ${dotClass}"></span>
                    ${isLow ? "Shortage (<75%)" : "Eligible"}
                </span>
            </td>
            <td style="text-align: right;">
                <div class="table-actions" style="justify-content: flex-end;">
                    <button
                        type="button"
                        class="icon-action-btn edit-btn"
                        data-id="${student.id}"
                        title="Edit Student"
                    >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                        </svg>
                        Edit
                    </button>

                    <button
                        type="button"
                        class="icon-action-btn delete delete-btn"
                        data-id="${student.id}"
                        data-name="${student.name}"
                        title="Remove Student"
                    >
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

    document.querySelectorAll(".edit-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            openEditStudentModal(btn.dataset.id);
        });
    });

    document.querySelectorAll(".delete-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            openDeleteModal(btn.dataset.id, btn.dataset.name);
        });
    });
}

function setupStudentModals() {
    const studentModal = document.getElementById("studentModal");
    const openAddBtn = document.getElementById("openAddStudentModal");
    const closeBtn = document.getElementById("closeStudentModal");
    const cancelBtn = document.getElementById("cancelStudentModal");
    const form = document.getElementById("studentForm");

    const deleteModal = document.getElementById("confirmDeleteModal");
    const closeDeleteBtn = document.getElementById("closeDeleteModal");
    const cancelDeleteBtn = document.getElementById("cancelDeleteModal");
    const confirmDeleteBtn = document.getElementById("confirmDeleteBtn");

    if (openAddBtn) {
        openAddBtn.addEventListener("click", () => {
            editingStudentId = null;

            document.getElementById("modalStudentTitle").textContent = "Add New Student";
            form.reset();

            studentModal.classList.add("active");
        });
    }

    const closeModal = () => {
        if (studentModal) {
            studentModal.classList.remove("active");
        }

        editingStudentId = null;

        if (form) {
            form.reset();
        }
    };

    if (closeBtn) {
        closeBtn.addEventListener("click", closeModal);
    }

    if (cancelBtn) {
        cancelBtn.addEventListener("click", closeModal);
    }

    if (form) {
        form.addEventListener("submit", event => {
            event.preventDefault();

            const roll = document.getElementById("formStudentRoll").value.trim();
            const name = document.getElementById("formStudentName").value.trim();
            const email = document.getElementById("formStudentEmail").value.trim();
            const dept = document.getElementById("formStudentDept").value;
            const sem = document.getElementById("formStudentSem").value;

            if (!roll || !name || !email || !dept || !sem) {
                showToast("Please fill in all student details.", "error");
                return;
            }

            const payload = {
                roll,
                name,
                email,
                department: dept,
                semester: sem
            };

            apiRequest(API_ENDPOINTS.STUDENTS, {
                method: "POST",
                body: JSON.stringify(payload)
            }).catch(err => console.warn("Notice: Student save API call:", err));

            if (editingStudentId) {
                const studentIndex = studentsData.findIndex(
                    student => student.id === editingStudentId
                );

                if (studentIndex === -1) {
                    showToast("Student record was not found.", "error");
                    return;
                }

                const duplicateId = studentsData.some(
                    student => student.id === roll && student.id !== editingStudentId
                );

                if (duplicateId) {
                    showToast("Another student already uses this roll number.", "error");
                    return;
                }

                studentsData[studentIndex] = {
                    ...studentsData[studentIndex],
                    id: roll,
                    name: name,
                    email: email,
                    department: dept,
                    semester: sem
                };

                showToast(`Student ${name} updated successfully!`, "success");
            } else {
                const studentExists = studentsData.some(
                    student => student.id.toLowerCase() === roll.toLowerCase()
                );

                if (studentExists) {
                    showToast("A student with this roll number already exists.", "error");
                    return;
                }

                studentsData.unshift({
                    id: roll,
                    name: name,
                    department: dept,
                    semester: sem,
                    email: email,
                    attendancePct: 100.0,
                    status: "Good Standing"
                });

                showToast(`Student ${name} added successfully!`, "success");
            }

            closeModal();

            if (window.applyStudentFilters) {
                window.applyStudentFilters();
            } else {
                renderStudentsTable(studentsData);
            }
        });
    }

    const hideDeleteModal = () => {
        if (deleteModal) {
            deleteModal.classList.remove("active");
        }

        activeDeleteStudentId = null;
    };

    if (closeDeleteBtn) {
        closeDeleteBtn.addEventListener("click", hideDeleteModal);
    }

    if (cancelDeleteBtn) {
        cancelDeleteBtn.addEventListener("click", hideDeleteModal);
    }

    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener("click", () => {
            if (!activeDeleteStudentId) return;

            apiRequest(
                `${API_ENDPOINTS.STUDENTS}/${encodeURIComponent(activeDeleteStudentId)}`,
                { method: "DELETE" }
            ).catch(err => console.warn("Notice: Student delete API call:", err));

            studentsData = studentsData.filter(
                student => student.id !== activeDeleteStudentId
            );

            hideDeleteModal();

            if (window.applyStudentFilters) {
                window.applyStudentFilters();
            } else {
                renderStudentsTable(studentsData);
            }

            showToast("Student record removed from active roster.", "info");
        });
    }
}

function openEditStudentModal(id) {
    const student = studentsData.find(student => student.id === id);

    if (!student) {
        showToast("Student record not found.", "error");
        return;
    }

    editingStudentId = student.id;

    document.getElementById("modalStudentTitle").textContent =
        `Edit Student: ${student.name}`;

    document.getElementById("formStudentRoll").value = student.id;
    document.getElementById("formStudentName").value = student.name;
    document.getElementById("formStudentEmail").value = student.email;
    document.getElementById("formStudentDept").value = student.department;
    document.getElementById("formStudentSem").value = student.semester;

    document.getElementById("studentModal").classList.add("active");
}

function openDeleteModal(id, name) {
    activeDeleteStudentId = id;

    document.getElementById("deleteTargetName").textContent = `${name} (${id})`;
    document.getElementById("confirmDeleteModal").classList.add("active");
}

function showToast(message, type = "info") {
    const container = document.getElementById("toastContainer");

    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 3200);
}