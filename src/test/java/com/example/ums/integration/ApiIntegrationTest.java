package com.example.ums.integration;

import com.example.ums.model.Role;
import com.example.ums.model.User;
import com.example.ums.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String PASSWORD = "pass1234";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        userRepository.deleteAll();
        saveUser("admin", Role.ADMIN);
        saveUser("teacher1", Role.TEACHER);
        saveUser("student1", Role.STUDENT);
    }

    // Integration test 1: login সফল, ভুল password 401, logout-এর পর session বাতিল
    @Test
    void loginWrongPasswordAndLogoutFlow() throws Exception {
        MvcResult ok = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        MockHttpSession session = (MockHttpSession) ok.getRequest().getSession(false);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));

        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();

        mvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    // Integration test 2: RBAC - teacher ও student শুধু নিজেদের resource পায়
    @Test
    void rbacRestrictsTeacherAndStudentToOwnResource() throws Exception {
        MockHttpSession teacher = login("teacher1");
        mvc.perform(get("/api/teachers").session(teacher)).andExpect(status().isOk());
        mvc.perform(get("/api/students").session(teacher)).andExpect(status().isForbidden());

        MockHttpSession student = login("student1");
        mvc.perform(get("/api/students").session(student)).andExpect(status().isOk());
        mvc.perform(get("/api/teachers").session(student)).andExpect(status().isForbidden());

        mvc.perform(get("/api/teachers")).andExpect(status().isUnauthorized());
    }

    // Integration test 3: admin একজন teacher তৈরি, পড়া, update, delete করে
    @Test
    void adminCreatesReadsUpdatesAndDeletesTeacher() throws Exception {
        MockHttpSession admin = login("admin");

        MvcResult created = mvc.perform(post("/api/teachers").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("karim", "Karim")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(get("/api/teachers/" + id).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("karim"));

        mvc.perform(put("/api/teachers/" + id).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teacherJson("karim", "Dr. Karim")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Dr. Karim"));

        mvc.perform(delete("/api/teachers/" + id).session(admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/teachers/" + id).session(admin))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private void saveUser(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setFullName(username);
        u.setEmail(username + "@kuet.ac.bd");
        u.setRole(role);
        userRepository.save(u);
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String teacherJson(String username, String fullName) {
        return """
                {"username":"%s","password":"%s","fullName":"%s",
                 "email":"%s@kuet.ac.bd","department":"CSE","designation":"Lecturer"}
                """.formatted(username, PASSWORD, fullName, username);
    }
}