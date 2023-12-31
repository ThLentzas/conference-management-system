package com.example.conference_management_system.conference;

import com.example.conference_management_system.entity.Conference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ConferenceRepository extends JpaRepository<Conference, UUID> {
    @Query("""
                SELECT COUNT(c) > 0
                FROM Conference c
                WHERE LOWER(c.name) = LOWER(:name)
            """)
    boolean existsByNameIgnoringCase(@Param("name") String name);

    @Query("""
                SELECT COUNT(c) > 0
                FROM Conference c
                JOIN c.users u
                WHERE c.id = :conferenceId AND u.id = :userId
            """)
    boolean isPc_ChairAtConference(@Param("conferenceId") UUID conferenceId, @Param("userId") Long userId);
}
