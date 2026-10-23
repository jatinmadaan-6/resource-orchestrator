package com.enterprise.orchestrator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_users")
@Getter @Setter @NoArgsConstructor
public class AppUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt hash

    @Column(nullable = false)
    private String fullName;

    private String email;
    private String department;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;

    /** The person who approves this user's requests. */
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser manager;

    public AppUser(String username, String password, String fullName, String email,
                   String department, Role role, AppUser manager) {
        this.username = username; this.password = password; this.fullName = fullName;
        this.email = email; this.department = department; this.role = role; this.manager = manager;
    }
}
