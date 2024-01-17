package com.example.conference_management_system.paper;

import com.example.conference_management_system.AbstractRepositoryTest;
import com.example.conference_management_system.entity.Paper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class PaperRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private PaperRepository underTest;

    //existsByTitleIgnoreCase()
    @Test
    void shouldReturnTrueWhenSearchingForAPaperThatExistsWithGivenTitleIgnoringCase() {
        //Arrange
        Paper paper = getPaper();
        this.underTest.save(paper);

        //Act & Assert
        assertThat(this.underTest.existsByTitleIgnoreCase("tiTle")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSearchingForAPaperThatDoesNotExistWithGivenTitleIgnoringCase() {
        //Arrange
        Paper paper = getPaper();
        this.underTest.save(paper);

        //Act & Assert
        assertThat(this.underTest.existsByTitleIgnoreCase("test")).isFalse();
    }

    private Paper getPaper() {
        Paper paper = new Paper();
        paper.setId(1L);
        paper.setTitle("title");
        paper.setAbstractText("abstractText");
        paper.setAuthors("author 1, author2");
        paper.setKeywords("keyword 1, keyword 2");

        return paper;
    }
}
