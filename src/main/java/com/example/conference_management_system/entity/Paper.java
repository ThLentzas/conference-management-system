package com.example.conference_management_system.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

import com.example.conference_management_system.paper.PaperState;

@Entity
@Table(name = "papers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"title"}, name = "unique_papers_title")
})
@Getter
@Setter
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
    @ManyToMany
    @JoinTable(
            name = "papers_users",
            joinColumns = @JoinColumn(name = "paper_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users;
    @ManyToOne
    private Conference conference;
    /*
        The relationship is bidirectional. I want to return all the reviews for a given a paper and the paper when a
        query from the reviews side.
     */
    @OneToMany(mappedBy = "paper")
    private Set<Review> reviews;

    /*
        When we query for a paper we also need to fetch the content for that paper(original file name, generated file
        name, extension)
     */
    @OneToOne(mappedBy = "paper")
    private Content content;

    public Paper() {
        this.state = PaperState.ACCEPTED;
    }

    public Paper(
            String title,
            String abstractText,
            String authors,
            String keywords,
            Set<User> users) {
        this.title = title;
        this.abstractText = abstractText;
        this.state = PaperState.ACCEPTED;
        this.authors = authors;
        this.keywords = keywords;
        this.users = users;
    }
}
