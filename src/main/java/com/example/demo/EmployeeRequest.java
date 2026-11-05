package com.example.demo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// What a client sends to create or update an employee. There is no id:
// the server assigns it, so a client cannot choose or overwrite one.
public record EmployeeRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull Long departmentId) {
}
