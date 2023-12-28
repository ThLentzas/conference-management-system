package com.example.conference_management_system.paper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import org.springframework.hateoas.RepresentationModel;


@Getter
@Setter
@AllArgsConstructor
public class PaperDTO extends RepresentationModel<PaperDTO> {
    private Long id;
    private LocalDate createdDate;
    private String title;
    private String abstractText;
    private String[] authors;
    private String[] keywords;
}
