package com.example.conference_management_system.review;

import com.example.conference_management_system.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("""
                SELECT COUNT(r) > 0
                FROM Review r
                WHERE r.paper.id = :paperId AND r.user.id = :userId
            """
    )
    boolean isReviewerAtPaper(@Param("paperId") Long paperId, @Param("userId") Long userId);

    @Query("""
                SELECT r
                FROM Review r
                WHERE r.paper.id = :paperId
            """
    )
    List<Review> findByPaperId(@Param("paperId") Long paperId);
}
