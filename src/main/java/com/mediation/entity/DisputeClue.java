package com.mediation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_clues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeClue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "area_name", nullable = false)
    private String areaName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "involved_people", nullable = false)
    private Integer involvedPeople;

    @Column(name = "involved_amount", precision = 12, scale = 2)
    private BigDecimal involvedAmount;

    @Column(name = "has_amount")
    private Boolean hasAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", nullable = false)
    private Dispute.DisputeType disputeType;

    @Column(name = "has_group_protest")
    private Boolean hasGroupProtest;

    @Enumerated(EnumType.STRING)
    @Column(name = "initial_risk_level")
    private RiskLevel initialRiskLevel;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_risk_level")
    private RiskLevel finalRiskLevel;

    @Column(name = "warning_id")
    private Long warningId;

    @Column(name = "has_dispute")
    private Boolean hasDispute;

    @Column(name = "dispute_id")
    private Long disputeId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.hasAmount == null) {
            this.hasAmount = false;
        }
        if (this.hasGroupProtest == null) {
            this.hasGroupProtest = false;
        }
        if (this.hasDispute == null) {
            this.hasDispute = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum RiskLevel {
        低, 中, 高, 极高
    }
}
