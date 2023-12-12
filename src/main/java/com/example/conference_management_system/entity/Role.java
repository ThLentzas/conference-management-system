package com.example.conference_management_system.entity;

import com.example.conference_management_system.role.RoleType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import lombok.ToString;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.io.Serializable;

/*
    The reason why both Role and User Entities have to implement Serializable is because they are part of the
    authentication object of the Security Context that is stored in Redis as the value of the SPRING_SECURITY_CONTEXT
    KEY
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@ToString
public class Role implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private RoleType type;
}