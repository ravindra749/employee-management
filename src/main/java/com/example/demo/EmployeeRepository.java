package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Load each employee's department in the same query.
    @Override
    @EntityGraph(attributePaths = "department")
    Page<Employee> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "department")
    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByDepartmentId(Long departmentId);
}
