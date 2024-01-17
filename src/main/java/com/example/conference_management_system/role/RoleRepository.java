package com.example.conference_management_system.role;

import com.example.conference_management_system.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @Query("""
                SELECT r
                FROM Role r
                WHERE r.type = :type
            """)
    Optional<Role> findByType(@Param("type") RoleType type);
}
