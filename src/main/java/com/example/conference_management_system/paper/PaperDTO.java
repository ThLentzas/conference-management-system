package com.example.conference_management_system.paper;

import com.example.conference_management_system.review.ReviewDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.core.io.Resource;

@Builder
@Getter
@Setter
public class PaperDTO {
    private Long id;
    private LocalDate createdDate;
    private String title;
    private String abstractText;
    private String[] authors;
    private String[] keywords;
    private PaperState state;
    private Set<ReviewDTO> reviews;
    private Resource file;
    //add conference
}
