package com.example.ums.controller;

import com.example.ums.model.Role;
import com.example.ums.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Access: ADMIN only. */
@RestController
@RequestMapping("/api/admins")
public class AdminController extends UserResourceController {

    public AdminController(UserService userService) {

        super(userService, Role.ADMIN);
    }
}
