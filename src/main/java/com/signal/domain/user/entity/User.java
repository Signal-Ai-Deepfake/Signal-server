package com.signal.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private String profileImageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String email, String password, String name, Integer age, Gender gender) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.createdAt = LocalDateTime.now();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfileImage(String url) {
        this.profileImageUrl = url;
    }

    public enum Gender {
        MALE, FEMALE, NONE
    }
}
