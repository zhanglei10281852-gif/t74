package com.mediation.repository;

import com.mediation.entity.Dispute;
import com.mediation.entity.DisputeClue;
import com.mediation.entity.DisputeClue.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DisputeClueRepository extends JpaRepository<DisputeClue, Long> {

    List<DisputeClue> findByReportId(Long reportId);

    List<DisputeClue> findByTaskId(Long taskId);

    Page<DisputeClue> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<DisputeClue> findByFinalRiskLevel(RiskLevel riskLevel, Pageable pageable);

    long countByFinalRiskLevel(RiskLevel riskLevel);

    @Query("SELECT c FROM DisputeClue c WHERE c.finalRiskLevel IN ('高', '极高') AND c.warningId IS NULL")
    List<DisputeClue> findHighRiskCluesWithoutWarning();

    @Query("SELECT c FROM DisputeClue c WHERE c.createdAt BETWEEN :startTime AND :endTime")
    List<DisputeClue> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT c.areaName, COUNT(c), SUM(c.riskScore) FROM DisputeClue c WHERE c.createdAt BETWEEN :startTime AND :endTime GROUP BY c.areaName ORDER BY SUM(c.riskScore) DESC")
    List<Object[]> getAreaHeatRank(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT c.organizationName, COUNT(c), SUM(c.riskScore) FROM DisputeClue c WHERE c.createdAt BETWEEN :startTime AND :endTime GROUP BY c.organizationName ORDER BY SUM(c.riskScore) DESC")
    List<Object[]> getOrganizationHeatRank(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    long countByDisputeType(Dispute.DisputeType disputeType);

    @Query("SELECT COUNT(c) FROM DisputeClue c WHERE c.createdAt BETWEEN :startTime AND :endTime")
    long countByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
