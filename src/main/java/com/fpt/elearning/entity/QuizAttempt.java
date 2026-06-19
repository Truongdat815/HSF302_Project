package com.fpt.elearning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    // Diem theo phan tram (0-100)
    private int score;

    private boolean passed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    public void prePersist() {
        this.attemptedAt = LocalDateTime.now();
    }
}
