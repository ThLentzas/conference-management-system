package com.example.conference_management_system.conference;

import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.key.ConferenceUserId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ConferenceUserRepository extends JpaRepository<ConferenceUser, ConferenceUserId> {

    @Query("""
                SELECT COUNT(cu) > 0
                FROM ConferenceUser cu
                WHERE cu.conference.id = :conferenceId AND cu.user.id = :userId
            """)
    boolean existsByConferenceIdAndUserId(@Param("conferenceId") UUID conferenceId,
                                          @Param("userId") Long userId);
}
