package com.example.demo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Transactional
    public Employee create(Employee employee) {
        employee.setId(null);
        if (employeeRepository.existsByEmailIgnoreCase(employee.getEmail())) {
            throw new DuplicateEmailException(employee.getEmail());
        }
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Long id, Employee updatedEmployee) {
        Employee employee = findById(id);
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(
                updatedEmployee.getEmail(), id)) {
            throw new DuplicateEmailException(updatedEmployee.getEmail());
        }
        employee.setFirstName(updatedEmployee.getFirstName());
        employee.setLastName(updatedEmployee.getLastName());
        employee.setEmail(updatedEmployee.getEmail());
        employee.setDepartment(updatedEmployee.getDepartment());
        return employeeRepository.save(employee);
    }

    @Transactional
    public void delete(Long id) {
        employeeRepository.delete(findById(id));
    }

    public List<Employee> findByDepartment(String department) {
        if (department == null || department.isBlank()) {
            return findAll();
        }
        return employeeRepository.findByDepartmentIgnoreCase(department.trim());
    }
}
