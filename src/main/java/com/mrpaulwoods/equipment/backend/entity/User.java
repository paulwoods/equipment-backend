package com.mrpaulwoods.equipment.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    // Null for accounts provisioned through Google sign-in, which have no local password.
    @JsonIgnore
    @Column
    private String password;

    // Google's stable subject identifier. Set once an account has signed in with
    // Google; matched on before email so a Google-side email change does not
    // strand the account.
    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<UserRole> userRoles = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "token_version", nullable = false)
    private long tokenVersion = 0L;

    @Version
    @Column(nullable = false)
    private long version;
}
