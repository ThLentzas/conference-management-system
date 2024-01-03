package com.example.conference_management_system.paper.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.paper.dto.PaperDTO;

import java.util.function.Function;

public class PaperDTOMapper implements Function<Paper, PaperDTO> {

    @Override
    public PaperDTO apply(Paper paper) {
        return new PaperDTO(
                paper.getId(),
                paper.getCreatedDate(),
                paper.getTitle(),
                paper.getAbstractText(),
                paper.getAuthors().split(","),
                paper.getKeywords().split(",")
        );
    }
}
