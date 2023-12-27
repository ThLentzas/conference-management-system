package com.example.conference_management_system.conference;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

import com.example.conference_management_system.entity.User;

@Getter
@Setter
@Builder
public class ConferenceDTO {
    private UUID id;
    private String name;
    private String description;
    private ConferenceState conferenceState;
    private Set<User> users;
}
