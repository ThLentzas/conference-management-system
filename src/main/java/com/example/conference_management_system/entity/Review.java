package com.example.conference_management_system.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @CreatedDate
    private LocalDate reviewedDate;
    @ManyToOne
    private Paper paper;
    @ManyToOne
    private User user;
    private String comment;
    private Double score;

    public Review() {
    }

    public Review(String comment, Double score) {
        this.comment = comment;
        this.score = score;
    }
}
