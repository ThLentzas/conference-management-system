package com.example.conference_management_system.entity.key;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class PaperUserId implements Serializable {
    private Long paperId;
    private Long userId;

    public PaperUserId() {
    }

    public PaperUserId(Long paperId, Long userId) {
        this.paperId = paperId;
        this.userId = userId;
    }
}