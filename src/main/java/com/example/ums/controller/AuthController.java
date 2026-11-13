package com.example.ums.controller;

import com.example.ums.dto.LoginRequest;
import com.example.ums.dto.UserResponse;
import com.example.ums.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/auth/login  - JSON login, creates a session (JSESSIONID cookie)
 * GET  /api/auth/me     - who am I
 * POST /api/auth/logout - handled by Spring Security (see SecurityConfig)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));

        SecurityContext context = contextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolderStrategy.setContext(context);

        // Protect against session fixation, then store the login in the session.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        securityContextRepository.saveContext(context, request, response);

        return userService.findByUsername(authentication.getName());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }
}
