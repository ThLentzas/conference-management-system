package com.example.conference_management_system.paper.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.paper.dto.PaperDTO;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaperDTOMapperTest {
    private PaperDTOMapper underTest;

    @BeforeEach
    void setup() {
        underTest = new PaperDTOMapper();
    }

    @Test
    void shouldMapPaperToPaperDTO() {
        String[] authors = {"author 1, author 2"};
        String[] keywords = {"keyword 1, keyword 2"};

        Paper paper = new Paper("title", "abstractText", String.join(",", authors), String.join(",", keywords));
        paper.setId(1L);
        paper.setCreatedDate(LocalDate.now());

        PaperDTO expected = new PaperDTO(
                1L,
                LocalDate.now(),
                "title",
                "abstractText",
                authors,
                keywords
        );

        PaperDTO actual = this.underTest.convert(paper);

        assertThat(actual).isEqualTo(expected);
    }
}
