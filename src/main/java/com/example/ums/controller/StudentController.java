package com.example.ums.controller;

import com.example.ums.model.Role;
import com.example.ums.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Access: ADMIN and STUDENT. */
@RestController
@RequestMapping("/api/students")
public class StudentController extends UserResourceController {

    public StudentController(UserService userService) {

        super(userService, Role.STUDENT);
    }
}
