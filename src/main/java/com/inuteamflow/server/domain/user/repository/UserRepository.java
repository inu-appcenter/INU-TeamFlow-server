package com.inuteamflow.server.domain.user.repository;

import com.inuteamflow.server.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByStudentNumber(String studentNumber);

    boolean existsByStudentNumber(String studentNumber);

    @Query("""
            SELECT u FROM User u
            WHERE u.name LIKE CONCAT('%', :name, '%') AND u.isSchoolVerified = true AND u.userId != :userId
            """)
    List<User> searchByName(@Param("name") String name, @Param("userId") Long userId);
}
