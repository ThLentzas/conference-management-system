package com.example.conference_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class Paper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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
    @OneToMany(mappedBy = "paper")
    private Set<PaperUser> paperUsers;
    @ManyToOne
    private Conference conference;
    /*
        When we query for a paper we also need to fetch the content for that paper(original file name, generated file
        name, extension)
     */
    @OneToOne(mappedBy = "paper")
    private Content content;
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
