package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_warnings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warning_no", unique = true, nullable = false)
    private String warningNo;

    @Column(name = "clue_id", nullable = false)
    private Long clueId;

    @Column(name = "clue_description", columnDefinition = "TEXT")
    private String clueDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_level", nullable = false)
    private DisputeClue.RiskLevel warningLevel;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "area_name", nullable = false)
    private String areaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarningStatus status;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "viewed_by")
    private String viewedBy;

    @Column(name = "intervened_at")
    private LocalDateTime intervenedAt;

    @Column(name = "intervened_by")
    private String intervenedBy;

    @Column(name = "mediator_id")
    private Long mediatorId;

    @Column(name = "mediator_name")
    private String mediatorName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "dispute_id")
    private Long disputeId;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = WarningStatus.已发出;
        }
        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum WarningStatus {
        已发出, 已关注, 已介入, 已化解, 已升级
    }
}
