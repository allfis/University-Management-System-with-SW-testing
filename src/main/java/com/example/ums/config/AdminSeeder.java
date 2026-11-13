package com.example.ums.config;

import com.example.ums.model.Role;
import com.example.ums.model.User;
import com.example.ums.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.username}") String username,
                       @Value("${app.admin.password}") String password,
                       @Value("${app.admin.email}") String email) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setFullName("System Administrator");
        admin.setEmail(email);
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Default admin '{}' created. Change its password after the first login.", username);
    }
}
