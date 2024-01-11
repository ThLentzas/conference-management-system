package com.example.conference_management_system.content;

import com.example.conference_management_system.entity.Content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {
    /*
        Since the relationship is @OneToOne, and we use @MapsId content and paper basically share the same PK.

        https://vladmihalcea.com/the-best-way-to-map-a-onetoone-relationship-with-jpa-and-hibernate/
     */
    @Query("""
                SELECT c
                FROM Content c
                JOIN FETCH c.paper
                WHERE c.id = :paperId
            """)
    Optional<Content> findByPaperId(@Param("paperId") Long paperId);
}
