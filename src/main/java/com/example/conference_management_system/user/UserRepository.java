package com.example.conference_management_system.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.conference_management_system.entity.User;

import java.util.Optional;

interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
                SELECT COUNT(u) > 0
                FROM User u
                WHERE LOWER(u.username) = LOWER(:username)
            """)
    boolean existsByUsernameIgnoreCase(@Param("username")String username);

    @Query("""
                SELECT u
                FROM User u
                WHERE u.username = :username
            """)
    Optional<User> findUserByUsername(@Param("username") String username);
}