package com.fpt.elearning.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    // RICH TEXT: noi dung bai hoc (HTML)
    @Column(columnDefinition = "TEXT")
    private String content;

    private String videoUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    // Cho phep hoc thu (preview) khi chua mua
    @Column(nullable = false)
    @Builder.Default
    private boolean preview = false;
}
