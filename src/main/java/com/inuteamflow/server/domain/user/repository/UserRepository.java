package com.inuteamflow.server.domain.user.repository;

import com.inuteamflow.server.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByStudentNumber(String studentNumber);
}
