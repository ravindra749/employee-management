"use strict";

const employeeForm = document.getElementById("employeeForm");
const employeeTableBody = document.getElementById("employeeTableBody");
const formHeading = document.getElementById("formHeading");
const formMessage = document.getElementById("message");
const listMessage = document.getElementById("listMessage");
const submitButton = document.getElementById("submitEmployee");
const cancelEditButton = document.getElementById("cancelEdit");
const loadEmployeesButton = document.getElementById("loadEmployees");
const departmentSelect = document.getElementById("departmentId");
const departmentFilter = document.getElementById("departmentFilter");
const clearDepartmentFilterButton = document.getElementById("clearDepartmentFilter");
const departmentForm = document.getElementById("departmentForm");
const departmentNameInput = document.getElementById("departmentName");
const departmentMessage = document.getElementById("departmentMessage");
const departmentList = document.getElementById("departmentList");
const previousPageButton = document.getElementById("previousPage");
const nextPageButton = document.getElementById("nextPage");
const pageInfo = document.getElementById("pageInfo");
const pageSizeSelect = document.getElementById("pageSize");
const sortButtons = document.querySelectorAll("button[data-sort]");
let employees = [];
let departments = [];
let currentPage = 0;
let totalPages = 0;
let sortField = "lastName";
let sortDirection = "asc";
let currentUser = null;
const currentUserLabel = document.getElementById("currentUser");
const signOutButton = document.getElementById("signOut");

function readEmployeeForm() {
    return {
        firstName: document.getElementById("firstName").value.trim(),
        lastName: document.getElementById("lastName").value.trim(),
        email: document.getElementById("email").value.trim(),
        departmentId: departmentSelect.value ? Number(departmentSelect.value) : null
    };
}

async function readError(response) {
    try {
        const body = await response.json();
        const fieldErrors = body.errors ? Object.values(body.errors).join(" ") : "";
        return body.message || fieldErrors || `Request failed (${response.status}).`;
    } catch {
        return `Request failed (${response.status}).`;
    }
}

// Spring Security puts the CSRF token in the XSRF-TOKEN cookie; changes must
// send it back in the X-XSRF-TOKEN header.
function csrfHeader() {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
    return match ? { "X-XSRF-TOKEN": decodeURIComponent(match[1]) } : {};
}

// Every API call goes through here: it adds the CSRF token to changes and
// sends the browser to the login page when the session has ended.
async function apiFetch(url, options = {}) {
    const method = (options.method ?? "GET").toUpperCase();
    const headers = { ...(options.headers ?? {}) };
    if (method !== "GET") {
        Object.assign(headers, csrfHeader());
    }
    const response = await fetch(url, { ...options, headers });
    if (response.status === 401) {
        window.location.assign("/login");
        throw new Error("Your session has ended. Please sign in again.");
    }
    if (response.status === 403) {
        throw new Error("You do not have permission to do that.");
    }
    return response;
}

async function loadCurrentUser() {
    const response = await apiFetch("/api/me");
    if (!response.ok) {
        throw new Error(await readError(response));
    }
    currentUser = await response.json();
    const isAdmin = currentUser.role === "ADMIN";
    document.body.classList.toggle("read-only", !isAdmin);
    currentUserLabel.textContent =
        `Signed in as ${currentUser.username} (${isAdmin ? "Admin" : "Viewer, read only"})`;
}

function resetEmployeeForm() {
    employeeForm.reset();
    delete employeeForm.dataset.employeeId;
    formHeading.textContent = "Add an employee";
    submitButton.textContent = "Save employee";
    cancelEditButton.hidden = true;
}

function addCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "";
    row.appendChild(cell);
    return cell;
}

function addOption(select, value, label) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = label;
    select.appendChild(option);
}

// Refill both department dropdowns, keeping what was selected.
function renderDepartmentOptions() {
    for (const [select, emptyLabel] of [
        [departmentSelect, "Choose a department"],
        [departmentFilter, "All departments"]
    ]) {
        const selected = select.value;
        select.replaceChildren();
        addOption(select, "", emptyLabel);
        for (const department of departments) {
            addOption(select, department.id, department.name);
        }
        select.value = departments.some((d) => String(d.id) === selected) ? selected : "";
    }
}

function renderDepartmentList() {
    departmentList.replaceChildren();
    for (const department of departments) {
        const item = document.createElement("li");
        const name = document.createElement("span");
        name.textContent = department.name;

        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.className = "text-button danger-button admin-only";
        deleteButton.dataset.departmentId = department.id;
        deleteButton.textContent = "Delete";
        deleteButton.setAttribute("aria-label", `Delete the ${department.name} department`);

        item.append(name, deleteButton);
        departmentList.appendChild(item);
    }
}

async function loadDepartments() {
    try {
        const response = await apiFetch("/api/departments");
        if (!response.ok) {
            throw new Error(await readError(response));
        }
        departments = await response.json();
        renderDepartmentOptions();
        renderDepartmentList();
        if (departments.length === 0) {
            departmentMessage.textContent = "Add a department before adding employees.";
        }
    } catch (error) {
        departmentMessage.textContent = `Could not load departments: ${error.message}`;
    }
}

function renderEmployees() {
    employeeTableBody.replaceChildren();

    for (const employee of employees) {
        const row = document.createElement("tr");
        addCell(row, employee.id);
        addCell(row, employee.firstName);
        addCell(row, employee.lastName);
        addCell(row, employee.email);
        addCell(row, employee.departmentName);

        const actions = document.createElement("td");
        actions.className = "row-actions admin-only";

        const editButton = document.createElement("button");
        editButton.type = "button";
        editButton.className = "text-button";
        editButton.dataset.action = "edit";
        editButton.dataset.employeeId = employee.id;
        editButton.textContent = "Edit";
        editButton.setAttribute("aria-label", `Edit ${employee.firstName} ${employee.lastName}`);

        const deleteButton = document.createElement("button");
        deleteButton.type = "button";
        deleteButton.className = "text-button danger-button";
        deleteButton.dataset.action = "delete";
        deleteButton.dataset.employeeId = employee.id;
        deleteButton.textContent = "Delete";
        deleteButton.setAttribute("aria-label", `Delete ${employee.firstName} ${employee.lastName}`);

        actions.append(editButton, deleteButton);
        row.appendChild(actions);
        employeeTableBody.appendChild(row);
    }
}

function renderPager() {
    pageInfo.textContent = `Page ${totalPages === 0 ? 0 : currentPage + 1} of ${totalPages}`;
    previousPageButton.disabled = currentPage === 0;
    nextPageButton.disabled = currentPage + 1 >= totalPages;
}

function renderSortHeaders() {
    for (const button of sortButtons) {
        const active = button.dataset.sort === sortField;
        button.closest("th").setAttribute(
            "aria-sort",
            active ? (sortDirection === "asc" ? "ascending" : "descending") : "none"
        );
    }
}

async function loadEmployees() {
    loadEmployeesButton.disabled = true;
    listMessage.textContent = "Loading employees…";

    try {
        // An empty filter lists everyone, so Refresh and the first load still work.
        const departmentId = departmentFilter.value;
        const departmentName = departmentFilter.selectedOptions[0]?.textContent ?? "";
        const params = new URLSearchParams({
            page: currentPage,
            size: pageSizeSelect.value,
            sort: sortField,
            direction: sortDirection
        });
        if (departmentId) {
            params.set("departmentId", departmentId);
        }
        const response = await apiFetch(`/api/employees?${params}`);
        if (!response.ok) {
            throw new Error(await readError(response));
        }

        const page = await response.json();
        totalPages = page.totalPages;
        // After a delete the last page can disappear; step back to the new last page.
        if (currentPage > 0 && currentPage >= totalPages) {
            currentPage = Math.max(totalPages - 1, 0);
            await loadEmployees();
            return;
        }
        employees = page.content;
        renderEmployees();
        renderPager();
        renderSortHeaders();
        if (page.totalElements === 0) {
            listMessage.textContent = departmentId
                ? `No employees found in the "${departmentName}" department.`
                : "No employees found. Add an employee to get started.";
        } else {
            const first = page.page * page.size + 1;
            const last = first + employees.length - 1;
            const range = `${first}–${last} of ${page.totalElements}`;
            listMessage.textContent = departmentId
                ? `Showing ${range} in the "${departmentName}" department.`
                : `Showing ${range} employees.`;
        }
    } catch (error) {
        listMessage.textContent = `Could not load employees: ${error.message}`;
    } finally {
        loadEmployeesButton.disabled = false;
    }
}

function reloadFromFirstPage() {
    currentPage = 0;
    loadEmployees();
}

employeeForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    submitButton.disabled = true;
    formMessage.textContent = "Saving employee…";

    const employeeId = employeeForm.dataset.employeeId;
    const isEditing = Boolean(employeeId);

    try {
        const response = await apiFetch(
            isEditing ? `/api/employees/${encodeURIComponent(employeeId)}` : "/api/employees",
            {
                method: isEditing ? "PUT" : "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(readEmployeeForm())
            }
        );

        if (!response.ok) {
            throw new Error(await readError(response));
        }

        const savedEmployee = await response.json();
        formMessage.textContent = isEditing
            ? `Updated ${savedEmployee.firstName} ${savedEmployee.lastName}.`
            : `Saved ${savedEmployee.firstName} ${savedEmployee.lastName}.`;
        resetEmployeeForm();
        await loadEmployees();
    } catch (error) {
        formMessage.textContent = `Could not save employee: ${error.message}`;
    } finally {
        submitButton.disabled = false;
    }
});

cancelEditButton.addEventListener("click", () => {
    resetEmployeeForm();
    formMessage.textContent = "Edit cancelled.";
});

employeeTableBody.addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    const employee = employees.find(
        (item) => String(item.id) === button.dataset.employeeId
    );
    if (!employee) {
        return;
    }

    if (button.dataset.action === "edit") {
        document.getElementById("firstName").value = employee.firstName;
        document.getElementById("lastName").value = employee.lastName;
        document.getElementById("email").value = employee.email;
        departmentSelect.value = String(employee.departmentId);
        employeeForm.dataset.employeeId = employee.id;
        formHeading.textContent = `Edit ${employee.firstName} ${employee.lastName}`;
        submitButton.textContent = "Save changes";
        cancelEditButton.hidden = false;
        formMessage.textContent = "Update the details, then save your changes.";
        document.getElementById("firstName").focus();
        return;
    }

    const fullName = `${employee.firstName} ${employee.lastName}`;
    if (!window.confirm(`Delete ${fullName}? This cannot be undone.`)) {
        return;
    }

    button.disabled = true;
    listMessage.textContent = `Deleting ${fullName}…`;
    try {
        const response = await apiFetch(
            `/api/employees/${encodeURIComponent(employee.id)}`,
            { method: "DELETE" }
        );
        if (!response.ok) {
            throw new Error(await readError(response));
        }

        if (employeeForm.dataset.employeeId === String(employee.id)) {
            resetEmployeeForm();
            formMessage.textContent = "The employee being edited was deleted.";
        }
        listMessage.textContent = `${fullName} was deleted.`;
        await loadEmployees();
    } catch (error) {
        listMessage.textContent = `Could not delete employee: ${error.message}`;
    }
});

departmentFilter.addEventListener("change", reloadFromFirstPage);

departmentFilterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    reloadFromFirstPage();
});

clearDepartmentFilterButton.addEventListener("click", () => {
    departmentFilter.value = "";
    reloadFromFirstPage();
});

// Clicking a column heading sorts by it; clicking it again reverses the order.
for (const button of sortButtons) {
    button.addEventListener("click", () => {
        if (sortField === button.dataset.sort) {
            sortDirection = sortDirection === "asc" ? "desc" : "asc";
        } else {
            sortField = button.dataset.sort;
            sortDirection = "asc";
        }
        reloadFromFirstPage();
    });
}

previousPageButton.addEventListener("click", () => {
    if (currentPage > 0) {
        currentPage -= 1;
        loadEmployees();
    }
});

nextPageButton.addEventListener("click", () => {
    if (currentPage + 1 < totalPages) {
        currentPage += 1;
        loadEmployees();
    }
});

pageSizeSelect.addEventListener("change", reloadFromFirstPage);

departmentForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = departmentNameInput.value.trim();
    departmentMessage.textContent = "Adding department…";
    try {
        const response = await apiFetch("/api/departments", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name })
        });
        if (!response.ok) {
            throw new Error(await readError(response));
        }
        const saved = await response.json();
        departmentForm.reset();
        departmentMessage.textContent = `Added the ${saved.name} department.`;
        await loadDepartments();
    } catch (error) {
        departmentMessage.textContent = `Could not add department: ${error.message}`;
    }
});

departmentList.addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-department-id]");
    if (!button) {
        return;
    }
    const department = departments.find(
        (item) => String(item.id) === button.dataset.departmentId
    );
    if (!department || !window.confirm(`Delete the ${department.name} department?`)) {
        return;
    }
    try {
        const response = await apiFetch(
            `/api/departments/${encodeURIComponent(department.id)}`,
            { method: "DELETE" }
        );
        if (!response.ok) {
            throw new Error(await readError(response));
        }
        departmentMessage.textContent = `Deleted the ${department.name} department.`;
        await loadDepartments();
        await loadEmployees();
    } catch (error) {
        departmentMessage.textContent = `Could not delete department: ${error.message}`;
    }
});

signOutButton.addEventListener("click", async () => {
    await fetch("/logout", { method: "POST", headers: csrfHeader() });
    window.location.assign("/login?logout");
});

loadEmployeesButton.addEventListener("click", loadEmployees);
loadCurrentUser()
    .then(loadDepartments)
    .then(loadEmployees)
    .catch((error) => {
        listMessage.textContent = `Could not start: ${error.message}`;
    });
