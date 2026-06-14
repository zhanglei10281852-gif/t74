package com.mediation.repository;

import com.mediation.entity.InvestigationTask;
import com.mediation.entity.InvestigationTask.TaskStatus;
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
public interface InvestigationTaskRepository extends JpaRepository<InvestigationTask, Long> {

    Page<InvestigationTask> findByStatus(TaskStatus status, Pageable pageable);

    Page<InvestigationTask> findByMediatorId(Long mediatorId, Pageable pageable);

    Page<InvestigationTask> findByOrganizationId(Long organizationId, Pageable pageable);

    Optional<InvestigationTask> findByTaskNo(String taskNo);

    boolean existsByOrganizationIdAndTaskMonth(Long organizationId, String taskMonth);

    @Query("SELECT t FROM InvestigationTask t WHERE t.taskMonth = :taskMonth")
    List<InvestigationTask> findByTaskMonth(@Param("taskMonth") String taskMonth);

    @Query("SELECT t FROM InvestigationTask t WHERE t.status = '待提交' AND t.deadline < :today")
    List<InvestigationTask> findOverdueTasks(@Param("today") LocalDate today);

    long countByStatus(TaskStatus status);

    long countByTaskMonth(String taskMonth);

    @Query("SELECT COUNT(t) FROM InvestigationTask t WHERE t.taskMonth = :taskMonth AND t.status = '已提交'")
    long countSubmittedByTaskMonth(@Param("taskMonth") String taskMonth);
}
