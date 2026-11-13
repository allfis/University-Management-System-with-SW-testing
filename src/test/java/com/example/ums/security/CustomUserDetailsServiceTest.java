package com.example.ums.security;

import com.example.ums.model.Role;
import com.example.ums.model.User;
import com.example.ums.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    // Unit test 5: role-কে ROLE_ prefix সহ authority বানায়
    @Test
    void loadUser_mapsRoleToRolePrefixedAuthority() {
        User teacher = new User();
        teacher.setUsername("t1");
        teacher.setPassword("HASH");
        teacher.setRole(Role.TEACHER);
        when(userRepository.findByUsername("t1")).thenReturn(Optional.of(teacher));

        UserDetails details = service.loadUserByUsername("t1");

        assertThat(details.getUsername()).isEqualTo("t1");
        assertThat(details.getPassword()).isEqualTo("HASH");

        List<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(authorities).containsExactly("ROLE_TEACHER");
    }

    // Unit test 6: user না থাকলে UsernameNotFoundException
    @Test
    void loadUser_unknownUsername_throwsUsernameNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}