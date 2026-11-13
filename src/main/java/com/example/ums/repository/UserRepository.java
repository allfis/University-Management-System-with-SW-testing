package com.example.ums.repository;

import com.example.ums.model.Role;
import com.example.ums.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findAllByRole(Role role);

    Optional<User> findByIdAndRole(Long id, Role role);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByRole(Role role);

    long countByRole(Role role);
}
