package com.example.demo;

// An employee request named a department id that does not exist.
public class UnknownDepartmentException extends RuntimeException {

    public UnknownDepartmentException(Long departmentId) {
        super("No department has id " + departmentId + ".");
    }
}
