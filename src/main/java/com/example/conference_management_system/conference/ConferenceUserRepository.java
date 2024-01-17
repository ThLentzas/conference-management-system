package com.example.conference_management_system.conference;

import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.key.ConferenceUserId;

import org.springframework.data.jpa.repository.JpaRepository;

interface ConferenceUserRepository extends JpaRepository<ConferenceUser, ConferenceUserId> {
}
