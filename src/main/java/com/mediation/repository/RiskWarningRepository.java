package com.mediation.repository;

import com.mediation.entity.DisputeClue.RiskLevel;
import com.mediation.entity.RiskWarning;
import com.mediation.entity.RiskWarning.WarningStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskWarningRepository extends JpaRepository<RiskWarning, Long> {

    Optional<RiskWarning> findByWarningNo(String warningNo);

    Optional<RiskWarning> findByClueId(Long clueId);

    Page<RiskWarning> findByStatus(WarningStatus status, Pageable pageable);

    Page<RiskWarning> findByWarningLevel(RiskLevel warningLevel, Pageable pageable);

    Page<RiskWarning> findByOrganizationId(Long organizationId, Pageable pageable);

    long countByStatus(WarningStatus status);

    long countByWarningLevel(RiskLevel warningLevel);

    @Query("SELECT w FROM RiskWarning w WHERE w.status = '已发出' AND w.issuedAt BETWEEN :startTime AND :endTime AND w.intervenedAt IS NOT NULL")
    List<RiskWarning> findIntervenedWarnings(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT w FROM RiskWarning w WHERE w.issuedAt BETWEEN :startTime AND :endTime")
    List<RiskWarning> findByIssuedAtBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(w) FROM RiskWarning w WHERE w.status = '已化解' AND w.mediatorId = :mediatorId")
    long countResolvedByMediatorId(@Param("mediatorId") Long mediatorId);

    @Query("SELECT COUNT(w) FROM RiskWarning w WHERE w.mediatorId = :mediatorId AND w.status IN ('已化解', '已升级')")
    long countTotalHandledByMediatorId(@Param("mediatorId") Long mediatorId);
}
