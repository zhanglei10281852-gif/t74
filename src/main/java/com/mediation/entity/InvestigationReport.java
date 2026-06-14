package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investigation_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "mediator_id", nullable = false)
    private Long mediatorId;

    @Column(name = "mediator_name", nullable = false)
    private String mediatorName;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "investigation_date", nullable = false)
    private LocalDate investigationDate;

    @Column(name = "investigation_area", nullable = false)
    private String investigationArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "investigation_method", nullable = false)
    private InvestigationMethod investigationMethod;

    @Column(name = "has_clue", nullable = false)
    private Boolean hasClue;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum InvestigationMethod {
        走访入户, 座谈了解, 信息摸排
    }
}
