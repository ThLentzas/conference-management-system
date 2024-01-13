package com.example.conference_management_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @CreatedDate
    private LocalDate reviewedDate;
    @ManyToOne(fetch = FetchType.LAZY)
    private Paper paper;
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
    private String comment;
    private Double score;

    public Review() {
    }

    public Review(Paper paper, User user, String comment, Double score) {
        this.paper = paper;
        this.user = user;
        this.comment = comment;
        this.score = score;
    }
}
