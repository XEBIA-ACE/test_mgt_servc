package com.example.usermanagement.repository;

import com.example.usermanagement.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /**
     * Full-text search across email, username, first name, and last name.
     * Case-insensitive using LOWER() — for large tables consider a DB-level full-text index.
     */
    @Query("""
        SELECT u FROM User u WHERE
            LOWER(u.email)     LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.username)  LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
