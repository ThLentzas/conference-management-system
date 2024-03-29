package com.example.conference_management_system.paper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import com.example.conference_management_system.entity.Paper;

/*
    We are using JOIN FETCH when it comes to paperUsers because there will always be a User associated with a paper but
    not always a paper be associated with a conference that's why we use LEFT JOIN in this case
 */
public interface PaperRepository extends JpaRepository<Paper, Long>, JpaSpecificationExecutor<Paper> {

    @Query("""
                SELECT COUNT(p) > 0
                FROM Paper p
                WHERE LOWER(p.title) = LOWER(:title)
            """)
    boolean existsByTitleIgnoreCase(@Param("title") String title);

    @Query("""
                SELECT p
                FROM Paper p
                JOIN FETCH p.paperUsers pu
                JOIN FETCH pu.user
                LEFT JOIN FETCH p.reviews r
                LEFT JOIN FETCH r.user u
                LEFT JOIN FETCH p.conference c
                LEFT JOIN FETCH c.conferenceUsers cu
                LEFT JOIN FETCH cu.user
                WHERE p.id = :id
            """)
    Optional<Paper> findPaperGraphById(@Param("id") Long id);

    @Query("""
                SELECT p
                FROM Paper p
                JOIN FETCH p.paperUsers pu
                JOIN FETCH pu.user u
                WHERE p.id = :id
            """)
    Optional<Paper> findByPaperIdFetchingPaperUsers(@Param("id") Long id);

    @Query("""
                SELECT p
                FROM Paper p
                JOIN FETCH p.paperUsers pu
                JOIN FETCH pu.user u
                LEFT JOIN FETCH p.reviews r
                LEFT JOIN FETCH r.user
                WHERE p.id = :id
            """)
    Optional<Paper> findByPaperIdFetchingPaperUsersAndReviews(@Param("id") Long id);

    @Query("""
                SELECT p
                FROM Paper p
                JOIN FETCH p.paperUsers pu
                JOIN FETCH pu.user u
                LEFT JOIN FETCH p.conference
                WHERE p.id = :id
            """)
    Optional<Paper> findByPaperIdFetchingPaperUsersAndConference(@Param("id") Long id);

    @Query("""
                SELECT p
                FROM Paper p
                LEFT JOIN FETCH p.reviews r
                LEFT JOIN FETCH r.user
                WHERE p IN :papers
            """)
    List<Paper> fetchReviewsForPapers(@Param("papers") List<Paper> papers);
}
