package com.example.conference_management_system.paper.mapper;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.paper.dto.PaperDTO;

// https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/convert/converter/Converter.html
import org.springframework.core.convert.converter.Converter;

public class PaperDTOMapper implements Converter<Paper, PaperDTO> {

    @Override
    public PaperDTO convert(Paper paper) {
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
