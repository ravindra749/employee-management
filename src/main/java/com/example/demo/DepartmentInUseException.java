package com.example.demo;

public class DepartmentInUseException extends RuntimeException {

    public DepartmentInUseException(String name) {
        super("The " + name + " department still has employees. "
                + "Move or delete them first.");
    }
}
