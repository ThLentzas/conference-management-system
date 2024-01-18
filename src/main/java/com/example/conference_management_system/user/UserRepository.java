package com.example.conference_management_system.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.conference_management_system.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
                SELECT COUNT(u) > 0
                FROM User u
                WHERE LOWER(u.username) = LOWER(:username)
            """)
    boolean existsByUsernameIgnoreCase(@Param("username") String username);

    @Query("""
                SELECT u
                FROM User u
                LEFT JOIN FETCH u.roles
                WHERE LOWER(u.username) = LOWER(:username)
            """)
    Optional<User> findUserByUsernameFetchingRoles(@Param("username") String username);

    @Query("""
                SELECT u
                FROM User u
                LEFT JOIN FETCH u.roles
                WHERE LOWER(u.fullName) = LOWER(:fullName)
            """)
    Optional<User> findUserByFullNameFetchingRoles(@Param("fullName") String fullName);

    @Query("""
                SELECT u
                FROM User u
                LEFT JOIN FETCH u.roles
                WHERE u.id = :id
            """)
    Optional<User> findUserByIdFetchingRoles(@Param("id") Long id);
}