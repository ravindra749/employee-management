"use strict";

const employeeForm = document.getElementById("employeeForm");
const employeeTableBody = document.getElementById("employeeTableBody");
const formHeading = document.getElementById("formHeading");
const formMessage = document.getElementById("message");
const listMessage = document.getElementById("listMessage");
const submitButton = document.getElementById("submitEmployee");
const cancelEditButton = document.getElementById("cancelEdit");
const loadEmployeesButton = document.getElementById("loadEmployees");
const departmentFilterForm = document.getElementById("departmentFilterForm");
const departmentFilter = document.getElementById("departmentFilter");
const clearDepartmentFilterButton = document.getElementById("clearDepartmentFilter");
let employees = [];

function readEmployeeForm() {
    return {
        firstName: document.getElementById("firstName").value.trim(),
        lastName: document.getElementById("lastName").value.trim(),
        email: document.getElementById("email").value.trim(),
        department: document.getElementById("department").value.trim()
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

function renderEmployees() {
    employeeTableBody.replaceChildren();

    for (const employee of employees) {
        const row = document.createElement("tr");
        addCell(row, employee.id);
        addCell(row, employee.firstName);
        addCell(row, employee.lastName);
        addCell(row, employee.email);
        addCell(row, employee.department);

        const actions = document.createElement("td");
        actions.className = "row-actions";

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

async function loadEmployees() {
    loadEmployeesButton.disabled = true;
    listMessage.textContent = "Loading employees…";

    try {
        // An empty filter lists everyone, so Refresh and the first load still work.
        const department = departmentFilter.value.trim();
        const url = department
            ? `/api/employees?department=${encodeURIComponent(department)}`
            : "/api/employees";
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(await readError(response));
        }

        employees = await response.json();
        renderEmployees();
        if (employees.length === 0) {
            listMessage.textContent = department
                ? `No employees found in the "${department}" department.`
                : "No employees found. Add an employee to get started.";
        } else {
            const count = `${employees.length} employee${employees.length === 1 ? "" : "s"}`;
            listMessage.textContent = department
                ? `Showing ${count} in the "${department}" department.`
                : `Showing ${count}.`;
        }
    } catch (error) {
        listMessage.textContent = `Could not load employees: ${error.message}`;
    } finally {
        loadEmployeesButton.disabled = false;
    }
}

employeeForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    submitButton.disabled = true;
    formMessage.textContent = "Saving employee…";

    const employeeId = employeeForm.dataset.employeeId;
    const isEditing = Boolean(employeeId);

    try {
        const response = await fetch(
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
        document.getElementById("department").value = employee.department;
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
        const response = await fetch(
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

departmentFilterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    loadEmployees();
});

clearDepartmentFilterButton.addEventListener("click", () => {
    departmentFilter.value = "";
    loadEmployees();
});

loadEmployeesButton.addEventListener("click", loadEmployees);
loadEmployees();
