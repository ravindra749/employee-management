package com.example.demo;

// What the API returns for a department.
public record DepartmentResponse(Long id, String name) {

    static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.getId(), department.getName());
    }
}
