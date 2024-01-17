package com.example.conference_management_system.paper;

import com.example.conference_management_system.entity.PaperUser;
import com.example.conference_management_system.entity.key.PaperUserId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperUserRepository extends JpaRepository<PaperUser, PaperUserId> {
}
