package com.example.demo;

// A query parameter such as page, size or sort has a value the API does not accept.
public class InvalidParameterException extends RuntimeException {

    private final String parameter;

    public InvalidParameterException(String parameter, String problem) {
        super(problem);
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
