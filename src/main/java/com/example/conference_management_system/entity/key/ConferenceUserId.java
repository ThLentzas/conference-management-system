package com.example.conference_management_system.entity.key;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class ConferenceUserId implements Serializable {
    private UUID conferenceId;
    private Long userId;

    public ConferenceUserId() {
    }

    public ConferenceUserId(UUID conferenceId, Long userId) {
        this.conferenceId = conferenceId;
        this.userId = userId;
    }
}
