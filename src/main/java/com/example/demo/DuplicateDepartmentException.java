package com.example.demo;

public class DuplicateDepartmentException extends RuntimeException {

    public DuplicateDepartmentException(String name) {
        super("A department named " + name + " already exists.");
    }
}
