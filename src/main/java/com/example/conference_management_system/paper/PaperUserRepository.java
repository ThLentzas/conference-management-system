package com.example.conference_management_system.paper;

import com.example.conference_management_system.entity.PaperUser;
import com.example.conference_management_system.entity.key.PaperUserId;
import com.example.conference_management_system.role.RoleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperUserRepository extends JpaRepository<PaperUser, PaperUserId> {

    @Query("""
                SELECT COUNT(pu) > 0
                FROM PaperUser pu
                WHERE pu.paper.id = :paperId AND pu.user.id = :userId AND pu.roleType = :roleType
            """)
    boolean existsByPaperIdUserIdAndRoleType(@Param("paperId") Long paperId,
                                             @Param("userId") Long userId,
                                             @Param("roleType") RoleType roleType);
}
