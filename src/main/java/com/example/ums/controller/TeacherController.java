package com.example.ums.controller;

import com.example.ums.model.Role;
import com.example.ums.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Access: ADMIN and TEACHER. */
@RestController
@RequestMapping("/api/teachers")
public class TeacherController extends UserResourceController {

    public TeacherController(UserService userService) {

        super(userService, Role.TEACHER);
    }
}
