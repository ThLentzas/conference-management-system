package com.example.conference_management_system.review;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.conference_management_system.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
