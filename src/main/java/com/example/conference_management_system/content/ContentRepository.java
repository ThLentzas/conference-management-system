package com.example.conference_management_system.content;

import com.example.conference_management_system.entity.Content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {
    @Query("""
                SELECT c
                FROM Content c
                WHERE c.paper.id = :paperId
            """)
    Optional<Content> findByPaperId(@Param("paperId") Long paperId);
}
