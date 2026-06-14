package com.mediation.repository;

import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeSource;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.Dispute.DisputeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Page<Dispute> findByStatus(DisputeStatus status, Pageable pageable);

    Page<Dispute> findByMediatorId(Long mediatorId, Pageable pageable);

    Page<Dispute> findByDisputeType(DisputeType disputeType, Pageable pageable);

    Page<Dispute> findBySource(DisputeSource source, Pageable pageable);

    Optional<Dispute> findByWarningId(Long warningId);

    Optional<Dispute> findByClueId(Long clueId);

    @Query("SELECT d FROM Dispute d WHERE d.applicantName LIKE %:keyword%")
    Page<Dispute> searchByApplicantName(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(DisputeStatus status);

    long countByDisputeType(DisputeType disputeType);

    long countBySource(DisputeSource source);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.source = '排查发现' AND d.warningId IS NOT NULL AND d.status IN ('调解成功', '已撤回')")
    long countSuccessfulPreventiveMediation();

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.source = '排查发现' AND d.warningId IS NOT NULL")
    long countTotalPreventiveMediation();
}
