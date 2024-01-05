package com.example.conference_management_system.entity;

import com.example.conference_management_system.conference.ConferenceState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "conferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"}, name = "unique_conference_name")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Conference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
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
    @OneToMany(mappedBy = "conference")
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
