package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mediation_organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediationOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String area;

    @Column(name = "area_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AreaType areaType;

    private String director;

    private String phone;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum AreaType {
        社区, 村组, 企业
    }
}
