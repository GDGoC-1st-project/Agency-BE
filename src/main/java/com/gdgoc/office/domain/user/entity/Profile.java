package com.gdgoc.office.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String bio;

    @Column(nullable = false, length = 100)
    private String major;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Department department;

    @Column(nullable = false, length = 200)
    private String contact;

    public Profile(User user, String bio, String major, Department department, String contact) {
        this.user = user;
        this.bio = bio;
        this.major = major;
        this.department = department;
        this.contact = contact;
    }

    public void update(String bio, String major, Department department, String contact) {
        if (bio != null) {
            this.bio = bio;
        }
        if (major != null) {
            this.major = major;
        }
        if (department != null) {
            this.department = department;
        }
        if (contact != null) {
            this.contact = contact;
        }
    }
}
