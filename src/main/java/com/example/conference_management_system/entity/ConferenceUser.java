package com.example.conference_management_system.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

import com.example.conference_management_system.entity.key.ConferenceUserId;

@Entity
@Table(name = "conferences_users")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class ConferenceUser {
    @EmbeddedId
    @EqualsAndHashCode.Include
    private ConferenceUserId id;
    @ManyToOne
    @MapsId("conferenceId")
    @JoinColumn(name = "conference_id")
    private Conference conference;
    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    @CreatedDate
    private LocalDate assignedDate;

    public ConferenceUser() {
    }

    public ConferenceUser(ConferenceUserId id, Conference conference, User user) {
        this.id = id;
        this.conference = conference;
        this.user = user;
    }
}
