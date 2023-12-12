package com.example.conference_management_system.entity;

import com.example.conference_management_system.paper.PaperState;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

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
    private String fileName;
    @ManyToMany
    @JoinTable(
            name = "papers_users",
            joinColumns = @JoinColumn(name = "papers_id"),
            inverseJoinColumns = @JoinColumn(name = "users_id")
    )
    private List<User> users;

    public Paper() {
        this.state = PaperState.CREATED;
    }
}
