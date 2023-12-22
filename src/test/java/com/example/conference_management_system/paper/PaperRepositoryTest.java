package com.example.conference_management_system.paper;

import com.example.conference_management_system.AbstractUnitTest;
import com.example.conference_management_system.entity.Paper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class PaperRepositoryTest extends AbstractUnitTest {
    @Autowired
    private PaperRepository underTest;

    @Test
    void shouldReturnTrueWhenSearchingForAPaperThatExistsWithGivenTitleIgnoringCase() {
        //Arrange
        Paper paper = new Paper("title", "abstractText", "author 1, author2", "keyword 1, keyword 2");
        this.underTest.save(paper);

        //Act & Assert
        assertThat(this.underTest.existsByTitleIgnoreCase("tiTle")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenSearchingForAPaperThatDoesNotExistWithGivenTitleIgnoringCase() {
        //Arrange
        Paper paper = new Paper("title", "abstractText", "author 1, author2", "keyword 1, keyword 2");
        this.underTest.save(paper);

        //Act & Assert
        assertThat(this.underTest.existsByTitleIgnoreCase("test")).isFalse();
    }
}
