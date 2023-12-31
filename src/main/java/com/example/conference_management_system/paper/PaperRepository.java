package com.example.conference_management_system.paper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.conference_management_system.entity.Paper;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    @Query("""
                SELECT COUNT(p) > 0
                FROM Paper p
                WHERE LOWER(p.title) = LOWER(:title)
            """)
    boolean existsByTitleIgnoreCase(@Param("title") String title);
}
