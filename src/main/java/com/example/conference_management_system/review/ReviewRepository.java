package com.example.conference_management_system.review;

import com.example.conference_management_system.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("""
                SELECT COUNT(r) > 0
                FROM Review r
                WHERE r.paper.id = :paperId AND r.user.id = :userId
            """
    )
    boolean isReviewer(@Param("paperId") Long paperId, @Param("userId") Long userId);
}
