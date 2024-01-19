package com.example.conference_management_system.conference;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import com.example.conference_management_system.entity.Conference;
import com.example.conference_management_system.entity.ConferenceUser;
import com.example.conference_management_system.entity.User;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

/*
    The findAll() endpoint can have optional query parameters to filter the conferences. We build the query dynamically
    and handling the potential N+1 query problems using Specifications and Criteria API.
 */
public class ConferenceSpecs implements Specification<Conference> {
    private final String name;
    private final String description;

    public ConferenceSpecs(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public Predicate toPredicate(Root<Conference> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();
        Fetch<Conference, ConferenceUser> conferenceUsersFetch = root.fetch("conferenceUsers", JoinType.INNER);
        Fetch<ConferenceUser, User> userFetch = conferenceUsersFetch.fetch("user", JoinType.INNER);
        userFetch.fetch("roles", JoinType.INNER);

        /*
            Case-insensitive searching
         */
        if (!this.name.isBlank()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                    "%" + this.name.toLowerCase() + "%"));
        }

        if (!this.description.isBlank()) {
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")),
                    "%" + this.description.toLowerCase() + "%"));
        }

        predicates.add(root.isNotNull());
        Predicate[] predicatesArr = predicates.toArray(new Predicate[0]);

        /*
            We combine our conditions with AND so the final query is something like
            WHERE c.name = :name AND c.description = :description AND c.id is not null
         */
        return criteriaBuilder.and(predicatesArr);
    }
}
