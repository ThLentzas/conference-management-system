package com.example.conference_management_system.entity;

import com.example.conference_management_system.conference.ConferenceState;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

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
    private ConferenceState state;
    @ManyToMany
    @JoinTable(
            name = "conferences_users",
            joinColumns = @JoinColumn(name = "conference_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users;
    @OneToMany(mappedBy = "conference")
    private Set<Paper> papers;

    public Conference() {
        this.state = ConferenceState.CREATED;
    }

    public Conference(String name, String description, Set<User> users) {
        this.name = name;
        this.description = description;
        this.users = users;
        this.state = ConferenceState.CREATED;
    }
}
