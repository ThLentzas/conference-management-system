package com.example.conference_management_system.paper;

import com.example.conference_management_system.entity.PaperUser;
import com.example.conference_management_system.entity.key.PaperUserId;
import com.example.conference_management_system.role.RoleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaperUserRepository extends JpaRepository<PaperUser, PaperUserId> {
}
