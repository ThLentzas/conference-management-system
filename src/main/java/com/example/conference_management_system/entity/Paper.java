package com.example.conference_management_system.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Set;

import com.example.conference_management_system.paper.PaperState;

@Entity
@Table(name = "papers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"title"}, name = "unique_papers_title")
})
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class Paper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    @CreatedDate
    private LocalDate createdDate;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String abstractText;
    @Column(nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private PaperState state;
    /*
        Will be stored as csv values. We can't have the names of the authors from the papers_users relationship because
        not all authors are registered users in our system.
     */
    @Column(nullable = false)
    private String authors;
    /*
        Will be stored as csv values.
     */
    private String keywords;
    @OneToMany(mappedBy = "paper", cascade = CascadeType.REMOVE)
    private Set<PaperUser> paperUsers;
    @ManyToOne(fetch = FetchType.LAZY)
    private Conference conference;
    @OneToMany(mappedBy = "paper")
    private Set<Review> reviews;

    public Paper() {
        this.state = PaperState.CREATED;
    }

    public Paper(
            String title,
            String abstractText,
            String authors,
            String keywords) {
        this.title = title;
        this.abstractText = abstractText;
        this.state = PaperState.CREATED;
        this.authors = authors;
        this.keywords = keywords;
    }
}
