package com.example.conference_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/*
    User is reserved word in psql. Anyway we use plural for naming tables.

    The reason why both Role and User Entities have to implement Serializable is because they are part of the
    authentication object of the Security Context that is stored in Redis as the value of the SPRING_SECURITY_CONTEXT
    KEY
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username"}, name = "unique_users_username")
})
@Getter
@Setter
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    /*
        By default, the Hibernate naming strategies would 1) Replace dots with underscores 2) Change camel case to snake
        case, 3) Lower-case table names. So for the property fullName that the corresponding attribute in the users
        table is also fullName without specifying the @Column(name = "fullname") would result in an error because the
        camel case of the fullName would be full_name and result in a column not found error. Better approach to use
        snake case and name our db column as full_name.
     */
    private String fullName;
    /*
        If we leave the default type of LAZY it will fail to get the roles. The UserDetails will be called and the
        transaction will end. In another session will try to grab the authorities, but it will fail since the
        transaction is closed.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
    @ManyToMany
    private Set<Paper> papers;

    /*
        Initially the user has no role
     */
    public User() {
        this.roles = new HashSet<>();
    }

    public User(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.roles = new HashSet<>();
    }
}