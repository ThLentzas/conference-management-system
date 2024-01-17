package com.example.conference_management_system.conference.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

import com.example.conference_management_system.user.dto.UserDTO;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ConferenceDTO {
    private UUID id;
    private String name;
    private String description;
    private Set<UserDTO> users;
}
