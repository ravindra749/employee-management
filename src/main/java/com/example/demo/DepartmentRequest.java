package com.example.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// What a client sends to create or rename a department.
public record DepartmentRequest(@NotBlank @Size(max = 255) String name) {
}
