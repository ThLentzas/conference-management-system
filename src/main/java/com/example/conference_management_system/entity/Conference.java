package com.example.conference_management_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "conferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"}, name = "unique_conference_name")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Conference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    @CreatedDate
    private LocalDate createdDate;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String description;
}
