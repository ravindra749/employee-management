package com.example.demo;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<DepartmentResponse> findAll() {
        return departmentRepository.findAll(Sort.by("name")).stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    public DepartmentResponse findById(Long id) {
        return DepartmentResponse.from(load(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateDepartmentException(name);
        }
        return DepartmentResponse.from(departmentRepository.save(new Department(name)));
    }

    @Transactional
    public DepartmentResponse rename(Long id, DepartmentRequest request) {
        Department department = load(id);
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateDepartmentException(name);
        }
        department.setName(name);
        return DepartmentResponse.from(departmentRepository.save(department));
    }

    // A department that still has employees cannot be deleted.
    @Transactional
    public void delete(Long id) {
        Department department = load(id);
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new DepartmentInUseException(department.getName());
        }
        departmentRepository.delete(department);
    }

    private Department load(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
    }
}
