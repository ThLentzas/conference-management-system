package com.example.conference_management_system.conference;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.conference_management_system.entity.Conference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConferenceRepository extends JpaRepository<Conference, UUID>, JpaSpecificationExecutor<Conference> {

    @Query("""
                SELECT COUNT(c) > 0
                FROM Conference c
                WHERE LOWER(c.name) = LOWER(:name)
            """)
    boolean existsByNameIgnoringCase(@Param("name") String name);

    @Query("""
                SELECT c
                FROM Conference c
                JOIN FETCH c.conferenceUsers cu
                JOIN FETCH cu.user
                WHERE c.id = :id
            """)
    Optional<Conference> findByConferenceIdFetchingConferenceUsers(@Param("id") UUID id);

    @Query("""
                SELECT c
                FROM Conference c
                WHERE c.name
                ILIKE (CONCAT('%', :name, '%'))
                ORDER BY c.name DESC
            """)
    List<Conference> findConferencesByNameContainingIgnoringCase(@Param("name") String name);

    @Query("""
                SELECT c
                FROM Conference c
                WHERE c.description
                ILIKE (CONCAT('%', :description, '%'))
                ORDER BY c.name DESC
            """)
    List<Conference> findConferencesByDescriptionContainingIgnoringCase(@Param("description") String description);

    @Query("""
                SELECT c
                FROM Conference c
                LEFT JOIN FETCH c.papers p
                LEFT JOIN FETCH p.reviews r
                LEFT JOIN FETCH r.user
                WHERE c IN :conferences
            """)
    List<Conference> fetchPapersForConferences(@Param("conferences") List<Conference> conferences);
}
