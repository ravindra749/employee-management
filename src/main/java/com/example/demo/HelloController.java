
package com.example.demo;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Demo endpoint from the agent's first edit; left out of the API docs.
@Hidden
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Welcome to our improved Employee Management System!";
    }
}
