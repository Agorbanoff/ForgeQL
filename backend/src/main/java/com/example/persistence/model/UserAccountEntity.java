package com.example.persistence.model;

import com.example.persistence.Enums.GlobalRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    @NotBlank
    @Size(min = 1, max = 20)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    @NotBlank
    @Email
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private GlobalRole globalRole;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "userAccount", fetch = FetchType.LAZY)
    private List<DataSourceEntity> dataSources = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<DataSourceAccessEntity> dataSourceAccesses = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.globalRole == null) {
            this.globalRole = GlobalRole.MEMBER;
        }
        this.createdAt = Instant.now();
    }
}
