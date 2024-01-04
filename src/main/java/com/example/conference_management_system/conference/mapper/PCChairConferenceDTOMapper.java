package com.example.conference_management_system.conference.mapper;

import com.example.conference_management_system.conference.dto.PCChairConferenceDTO;
import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.paper.dto.PCChairPaperDTO;
import com.example.conference_management_system.review.dto.PCChairReviewDTO;
import com.example.conference_management_system.user.dto.UserDTO;
import com.example.conference_management_system.user.UserDTOMapper;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PCChairConferenceDTOMapper implements Function<Conference, PCChairConferenceDTO> {
    private final UserDTOMapper userDTOMapper = new UserDTOMapper();

    @Override
    public PCChairConferenceDTO apply(Conference conference) {
        Set<UserDTO> users = conference.getUsers()
                .stream()
                .map(this.userDTOMapper)
                .collect(Collectors.toSet());

        Set<PCChairPaperDTO> papers = conference.getPapers().stream()
                .map(paper -> {
                    Set<PCChairReviewDTO> reviews = paper.getReviews().stream()
                            .map(review -> new PCChairReviewDTO(
                                    review.getId(),
                                    review.getPaper().getId(),
                                    review.getAssignedDate(),
                                    review.getReviewedDate(),
                                    review.getComment(),
                                    review.getScore(),
                                    review.getUser().getUsername()
                            )).collect(Collectors.toSet());

                    return new PCChairPaperDTO(
                            paper.getId(),
                            paper.getCreatedDate(),
                            paper.getTitle(),
                            paper.getAbstractText(),
                            paper.getAuthors().split(","),
                            paper.getKeywords().split(","),
                            paper.getState(),
                            reviews);
                }).collect(Collectors.toSet());

        return new PCChairConferenceDTO(
                conference.getId(),
                conference.getName(),
                conference.getDescription(),
                users,
                conference.getState(),
                papers
        );
    }
}
