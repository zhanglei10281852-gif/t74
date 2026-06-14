package com.mediation.repository;

import com.mediation.entity.InvestigationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvestigationReportRepository extends JpaRepository<InvestigationReport, Long> {

    Optional<InvestigationReport> findByTaskId(Long taskId);

    Page<InvestigationReport> findByMediatorId(Long mediatorId, Pageable pageable);

    Page<InvestigationReport> findByOrganizationId(Long organizationId, Pageable pageable);

    @Query("SELECT r FROM InvestigationReport r WHERE r.investigationDate BETWEEN :startDate AND :endDate")
    List<InvestigationReport> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM InvestigationReport r WHERE r.hasClue = true AND r.investigationDate BETWEEN :startDate AND :endDate")
    long countReportsWithClues(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
