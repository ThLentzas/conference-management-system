package com.example.conference_management_system.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.example.conference_management_system.conference.ConferenceState;

@Entity
@Table(name = "conferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"}, name = "unique_conference_name")
})
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Conference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    @CreatedDate
    private LocalDate createdDate;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private ConferenceState state;
    @OneToMany(mappedBy = "conference", cascade = CascadeType.REMOVE)
    private Set<ConferenceUser> conferenceUsers;
    @OneToMany(mappedBy = "conference")
    private Set<Paper> papers;

    public Conference() {
        this.state = ConferenceState.CREATED;
    }

    public Conference(String name, String description) {
        this.name = name;
        this.description = description;
        this.state = ConferenceState.CREATED;
    }
}
