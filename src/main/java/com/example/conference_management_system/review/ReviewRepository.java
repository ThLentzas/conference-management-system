package com.example.conference_management_system.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import com.example.conference_management_system.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("""
                SELECT r
                FROM Review r
                WHERE r.paper.id = :paperId
            """)
    List<Review> findByPaperId(@Param("paperId") Long paperId);
}
