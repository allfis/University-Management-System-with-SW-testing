package com.example.ums.service;

import com.example.ums.dto.UserRequest;
import com.example.ums.dto.UserResponse;
import com.example.ums.exception.ConflictException;
import com.example.ums.model.Role;
import com.example.ums.model.User;
import com.example.ums.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    private UserRequest teacherRequest(String username) {
        return new UserRequest(username, "pass1234", "Dr. Rahim", "rahim@kuet.ac.bd",
                "CSE", "Professor", null, null);
    }

    // Unit test 1: create -> password hash হয়, role ও teacher field সেট হয়
    @Test
    void create_hashesPasswordAndCopiesTeacherFields() {
        when(userRepository.existsByUsername("t1")).thenReturn(false);
        when(userRepository.existsByEmail("rahim@kuet.ac.bd")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("HASHED");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = service.create(Role.TEACHER, teacherRequest("  t1  "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("t1");
        assertThat(saved.getPassword()).isEqualTo("HASHED");
        assertThat(saved.getRole()).isEqualTo(Role.TEACHER);
        assertThat(saved.getDesignation()).isEqualTo("Professor");
        assertThat(result.role()).isEqualTo(Role.TEACHER);
    }

    // Unit test 2: username আগে থেকে থাকলে ConflictException, save হয় না
    @Test
    void create_duplicateUsername_throwsConflictAndNeverSaves() {
        when(userRepository.existsByUsername("t1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(Role.TEACHER, teacherRequest("t1")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username already taken");

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    // Unit test 3: update-এ password ফাঁকা থাকলে পুরনো hash থাকে
    @Test
    void update_withBlankPassword_keepsOldHash() {
        User existing = new User();
        existing.setId(5L);
        existing.setUsername("t1");
        existing.setPassword("OLD_HASH");
        existing.setEmail("rahim@kuet.ac.bd");
        existing.setFullName("Dr. Rahim");
        existing.setRole(Role.TEACHER);

        when(userRepository.findByIdAndRole(5L, Role.TEACHER)).thenReturn(Optional.of(existing));
        when(userRepository.existsByUsernameAndIdNot("t1", 5L)).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("rahim@kuet.ac.bd", 5L)).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserRequest request = new UserRequest("t1", "", "Dr. Rahim Updated", "rahim@kuet.ac.bd",
                "CSE", "Associate Professor", null, null);

        UserResponse result = service.update(Role.TEACHER, 5L, request);

        assertThat(existing.getPassword()).isEqualTo("OLD_HASH");
        assertThat(existing.getFullName()).isEqualTo("Dr. Rahim Updated");
        assertThat(result.designation()).isEqualTo("Associate Professor");
        verifyNoInteractions(passwordEncoder);
    }

    // Unit test 4: শেষ admin delete করা যাবে না
    @Test
    void delete_lastAdmin_throwsConflictAndNeverDeletes() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);

        when(userRepository.findByIdAndRole(1L, Role.ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(Role.ADMIN, 1L))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).delete(any(User.class));
    }
}