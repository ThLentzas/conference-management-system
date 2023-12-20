package com.example.conference_management_system.content;

import com.example.conference_management_system.entity.Content;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Long> {
}
