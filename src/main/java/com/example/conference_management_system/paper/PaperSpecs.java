package com.example.conference_management_system.paper;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import com.example.conference_management_system.entity.Paper;
import com.example.conference_management_system.entity.PaperUser;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

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
        The findAll() endpoint can have optional query parameters to filter the papers. We build the query dynamically
        and handling the potential N+1 query problems using Specifications and Criteria API.

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
