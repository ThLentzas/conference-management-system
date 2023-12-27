package com.example.conference_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import lombok.Getter;
import lombok.Setter;

/*
    Content is the entity we keep in our database as a reference with the file that we stored in our file system. The
    file is the pdf or latex file for each paper.
 */
@Entity
@Getter
@Setter
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String originalFileName;
    @Column(nullable = false)
    private String generatedFileName;
    @Column(nullable = false)
    private String fileExtension;
    @OneToOne
    @JoinColumn(name = "paper_id")
    private Paper paper;

    public Content() {
    }

    public Content(String originalFileName, String generatedFileName, String fileExtension) {
        this.originalFileName = originalFileName;
        this.generatedFileName = generatedFileName;
        this.fileExtension = fileExtension;
    }
}
