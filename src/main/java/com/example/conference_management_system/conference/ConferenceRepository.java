package com.example.conference_management_system.conference;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.conference_management_system.entity.Conference;

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
                JOIN FETCH c.conferenceUsers cu
                JOIN FETCH cu.user
                LEFT JOIN FETCH c.papers
                WHERE c.id = :id
            """)
    Optional<Conference> findByConferenceIdFetchingConferenceUsersAndPapers(@Param("id") UUID id);

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
