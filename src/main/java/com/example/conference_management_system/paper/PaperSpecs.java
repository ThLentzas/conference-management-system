package com.example.conference_management_system.paper;

import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.PaperUser;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PaperSpecs implements Specification<Paper> {
    private final String title;
    private final String author;
    private final String abstractText;

    public PaperSpecs(String title, String author, String abstractText) {
        this.title = title;
        this.author = author;
        this.abstractText = abstractText;
    }

    /*
        The equivalent JPQL query for the fetch setup is something like

                SELECT p
                FROM Paper p
                JOIN FETCH p.paperUsers pu
                JOIN FETCH pu.user
     */
    @Override
    public Predicate toPredicate(Root<Paper> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();
        Fetch<Paper, PaperUser> paperUsersFetch = root.fetch("paperUsers", JoinType.INNER);
        paperUsersFetch.fetch("user", JoinType.INNER);

        if (!this.title.isBlank()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),
                    "%" + this.title.toLowerCase() + "%"));
        }

        if (!this.author.isBlank()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("authors")),
                    "%" + this.author.toLowerCase() + "%"));
        }

        if (!this.abstractText.isBlank()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("abstractText")),
                    "%" + this.abstractText.toLowerCase() + "%"));
        }

        predicates.add(root.isNotNull());
        Predicate[] predicatesArr = predicates.toArray(new Predicate[0]);

        return criteriaBuilder.and(predicatesArr);
    }
}
