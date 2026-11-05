package com.example.demo;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Departments", description = "Manage the departments employees belong to")
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<DepartmentResponse> getDepartments() {
        return departmentService.findAll();
    }

    @GetMapping("/{id}")
    public DepartmentResponse getDepartmentById(@PathVariable Long id) {
        return departmentService.findById(id);
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> addDepartment(
            @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.create(request));
    }

    @PutMapping("/{id}")
    public DepartmentResponse renameDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        return departmentService.rename(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
