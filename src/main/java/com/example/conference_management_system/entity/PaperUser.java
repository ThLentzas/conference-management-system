package com.example.conference_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import com.example.conference_management_system.entity.key.PaperUserId;
import com.example.conference_management_system.role.RoleType;

import java.time.LocalDate;

@Entity
@Table(name = "papers_users")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class PaperUser {
    @EmbeddedId
    private PaperUserId id;
    @ManyToOne
    @MapsId("paperId")
    @JoinColumn(name = "paper_id")
    private Paper paper;
    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    @CreatedDate
    private LocalDate assignedDate;
    @Column(nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private RoleType roleType;

    public PaperUser() {
    }

    public PaperUser(PaperUserId id, Paper paper, User user, RoleType roleType) {
        this.id = id;
        this.paper = paper;
        this.user = user;
        this.roleType = roleType;
    }
}
