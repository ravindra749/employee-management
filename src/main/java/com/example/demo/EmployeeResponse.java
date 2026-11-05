package com.example.demo;

// What the API returns for an employee, including the department's name
// so the page does not need a second request to show it.
public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Long departmentId,
        String departmentName) {

    static EmployeeResponse from(Employee employee) {
        Department department = employee.getDepartment();
        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                department.getId(),
                department.getName());
    }
}
