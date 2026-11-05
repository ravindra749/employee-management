package com.example.demo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({EmployeeNotFoundException.class, DepartmentNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationFailure(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new TreeMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(
                        error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(errorBody("Validation failed", fieldErrors));
    }

    @ExceptionHandler(UnknownDepartmentException.class)
    public ResponseEntity<Map<String, Object>> handleUnknownDepartment(
            UnknownDepartmentException exception) {
        return ResponseEntity.badRequest().body(errorBody(
                "Validation failed",
                Map.of("departmentId", "does not match an existing department")));
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidParameter(
            InvalidParameterException exception) {
        return ResponseEntity.badRequest().body(errorBody(
                "Invalid query parameter",
                Map.of(exception.getParameter(), exception.getMessage())));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(
            DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(
                exception.getMessage(),
                Map.of("email", "is already used by another employee")));
    }

    @ExceptionHandler(DuplicateDepartmentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateDepartment(
            DuplicateDepartmentException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(
                exception.getMessage(),
                Map.of("name", "is already used by another department")));
    }

    @ExceptionHandler(DepartmentInUseException.class)
    public ResponseEntity<Map<String, String>> handleDepartmentInUse(
            DepartmentInUseException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    // The unique constraints still guard simultaneous requests that both
    // pass the service checks.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataConflict(
            DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message",
                "The request conflicts with existing data, such as an email or department name that is already in use."));
    }

    private static Map<String, Object> errorBody(String message, Map<String, String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("errors", errors);
        return body;
    }
}
