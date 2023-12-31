package com.example.conference_management_system.conference.dto;

import com.example.conference_management_system.user.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ConferenceDTO {
    private UUID id;
    private String name;
    private String description;
    private Set<UserDTO> users;
}
