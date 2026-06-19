package com.fpt.elearning.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // VD: ROLE_STUDENT, ROLE_ADMIN
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
