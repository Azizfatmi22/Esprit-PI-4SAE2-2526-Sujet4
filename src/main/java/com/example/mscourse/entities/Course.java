package com.example.mscourse.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity // This tells Spring this is a database table
@Table(name = "courses") // Table name in database
@Data // Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Generates empty constructor
@AllArgsConstructor // Generates constructor with all fields
public class Course {

    @Id // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    private Long id;

    @Column(nullable = false) // Cannot be null
    private String title;

    @Column(length = 2000) // Max length 2000 characters
    private String description;

    @Enumerated(EnumType.STRING) // Store enum as string in DB
    @Column(nullable = false)
    private Level level;

    @Column(nullable = false)
    private Double price;

    private Integer duration; // in minutes

    private String status; // DRAFT, PUBLISHED, ARCHIVED

    @Column(name = "trainer_id")
    private Long trainerId;

    private Integer enrolledStudents;

    private Double rating;

    private String thumbnail;

    // Relationships
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseAttachment> attachments = new ArrayList<>();

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // This method runs before inserting into database
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // This method runs before updating in database
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}