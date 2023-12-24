package com.example.conference_management_system.paper;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

import com.example.conference_management_system.entity.Review;

import org.springframework.core.io.Resource;

@Builder
@Getter
@Setter
public class PaperDTO {
    private LocalDate createdDate;
    private String title;
    private String abstractText;
    private String[] authors;
    private String[] keywords;
    private PaperState state;
    private Double score;
    private Set<Review> reviews;
    private Resource file;
    //add conference
}
