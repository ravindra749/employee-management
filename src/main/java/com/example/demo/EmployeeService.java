package com.example.demo;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    static final int MAX_PAGE_SIZE = 100;

    // API sort names mapped to entity properties; anything else is rejected.
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "firstName", "firstName",
            "lastName", "lastName",
            "email", "email",
            "departmentName", "department.name");

    // One page of everyone, or of one department when departmentId is set.
    public PageResponse<EmployeeResponse> findPage(
            Long departmentId, int page, int size, String sort, String direction) {
        Pageable pageable = pageRequest(page, size, sort, direction);
        Page<Employee> employees = departmentId == null
                ? employeeRepository.findAll(pageable)
                : employeeRepository.findByDepartmentId(departmentId, pageable);
        return PageResponse.from(employees.map(EmployeeResponse::from));
    }

    public EmployeeResponse findById(Long id) {
        return EmployeeResponse.from(load(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Employee employee = new Employee();
        copy(request, employee);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = load(id);
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new DuplicateEmailException(request.email());
        }
        copy(request, employee);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        employeeRepository.delete(load(id));
    }

    private static Pageable pageRequest(int page, int size, String sort, String direction) {
        if (page < 0) {
            throw new InvalidParameterException("page", "must be 0 or more");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidParameterException(
                    "size", "must be between 1 and " + MAX_PAGE_SIZE);
        }
        String property = SORT_FIELDS.get(sort);
        if (property == null) {
            throw new InvalidParameterException(
                    "sort", "must be one of id, firstName, lastName, email, departmentName");
        }
        Sort.Direction order;
        if ("asc".equalsIgnoreCase(direction)) {
            order = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            order = Sort.Direction.DESC;
        } else {
            throw new InvalidParameterException("direction", "must be asc or desc");
        }
        // Sorting by id second keeps the order stable when values repeat.
        return PageRequest.of(page, size, Sort.by(order, property).and(Sort.by("id")));
    }

    private Employee load(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private void copy(EmployeeRequest request, Employee employee) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new UnknownDepartmentException(request.departmentId()));
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setDepartment(department);
    }
}
