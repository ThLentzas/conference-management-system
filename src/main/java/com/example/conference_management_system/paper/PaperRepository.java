package com.example.conference_management_system.paper;

import com.example.conference_management_system.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    @Query("""
                SELECT COUNT(p) > 0
                FROM Paper p
                WHERE LOWER(p.title) = LOWER(:title)
            """)
    boolean existsByTitleIgnoreCase(@Param("title") String title);
}
