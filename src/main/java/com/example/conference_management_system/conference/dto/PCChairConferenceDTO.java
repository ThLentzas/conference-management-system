package com.example.conference_management_system.conference.dto;

import com.example.conference_management_system.conference.ConferenceState;
import com.example.conference_management_system.paper.dto.PCChairPaperDTO;
import com.example.conference_management_system.user.UserDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class PCChairConferenceDTO extends ConferenceDTO {
    private ConferenceState conferenceState;
    private Set<PCChairPaperDTO> papers;

    public PCChairConferenceDTO(
            UUID id,
            String name,
            String description,
            Set<UserDTO> users,
            ConferenceState conferenceState,
            Set<PCChairPaperDTO> papers) {
        super(id, name, description, users);
        this.conferenceState = conferenceState;
        this.papers = papers;
    }
}
