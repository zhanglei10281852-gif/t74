package com.mediation.controller;

import com.mediation.dto.DisputeClueDTO;
import com.mediation.dto.InvestigationReportDTO;
import com.mediation.entity.Dispute;
import com.mediation.entity.DisputeClue;
import com.mediation.entity.DisputeClue.RiskLevel;
import com.mediation.entity.InvestigationReport;
import com.mediation.entity.InvestigationReport.InvestigationMethod;
import com.mediation.entity.InvestigationTask;
import com.mediation.entity.InvestigationTask.TaskStatus;
import com.mediation.entity.RiskWarning;
import com.mediation.repository.DisputeClueRepository;
import com.mediation.repository.InvestigationReportRepository;
import com.mediation.repository.InvestigationTaskRepository;
import com.mediation.service.RiskAssessmentService;
import com.mediation.service.WarningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/investigation-reports")
@RequiredArgsConstructor
public class InvestigationReportController {

    private final InvestigationReportRepository reportRepository;
    private final InvestigationTaskRepository taskRepository;
    private final DisputeClueRepository clueRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final WarningService warningService;

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> submitReport(@Valid @RequestBody InvestigationReportDTO dto) {
        Optional<InvestigationTask> taskOpt = taskRepository.findById(dto.getTaskId());
        if (taskOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "排查任务不存在"));
        }

        InvestigationTask task = taskOpt.get();
        if (task.getStatus() == TaskStatus.已提交) {
            return ResponseEntity.badRequest().body(Map.of("error", "该任务已提交排查报告"));
        }

        InvestigationMethod method;
        try {
            method = InvestigationMethod.valueOf(dto.getInvestigationMethod());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的排查方式"));
        }

        if (dto.getHasClue() && (dto.getClues() == null || dto.getClues().isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of("error", "发现线索时必须填写线索详情"));
        }

        if (!dto.getHasClue()) {
            dto.setClues(new ArrayList<>());
        }

        InvestigationReport report = InvestigationReport.builder()
                .taskId(task.getId())
                .mediatorId(task.getMediatorId())
                .mediatorName(task.getMediatorName())
                .organizationId(task.getOrganizationId())
                .organizationName(task.getOrganizationName())
                .investigationDate(dto.getInvestigationDate())
                .investigationArea(dto.getInvestigationArea())
                .investigationMethod(method)
                .hasClue(dto.getHasClue())
                .remark(dto.getRemark())
                .build();

        InvestigationReport savedReport = reportRepository.save(report);

        List<DisputeClue> savedClues = new ArrayList<>();
        List<RiskWarning> generatedWarnings = new ArrayList<>();

        for (DisputeClueDTO clueDTO : dto.getClues()) {
            Dispute.DisputeType disputeType;
            try {
                disputeType = Dispute.DisputeType.valueOf(clueDTO.getDisputeType());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的纠纷类型: " + clueDTO.getDisputeType()));
            }

            RiskLevel initialRiskLevel = null;
            if (clueDTO.getInitialRiskLevel() != null) {
                try {
                    initialRiskLevel = RiskLevel.valueOf(clueDTO.getInitialRiskLevel());
                } catch (IllegalArgumentException ignored) {
                }
            }

            DisputeClue clue = DisputeClue.builder()
                    .reportId(savedReport.getId())
                    .taskId(task.getId())
                    .organizationId(task.getOrganizationId())
                    .organizationName(task.getOrganizationName())
                    .areaName(clueDTO.getAreaName() != null ? clueDTO.getAreaName() : dto.getInvestigationArea())
                    .description(clueDTO.getDescription())
                    .involvedPeople(clueDTO.getInvolvedPeople())
                    .involvedAmount(clueDTO.getInvolvedAmount())
                    .hasAmount(clueDTO.getHasAmount())
                    .disputeType(disputeType)
                    .hasGroupProtest(clueDTO.getHasGroupProtest())
                    .initialRiskLevel(initialRiskLevel)
                    .build();

            int riskScore = riskAssessmentService.calculateRiskScore(clue);
            RiskLevel finalRiskLevel = riskAssessmentService.determineRiskLevel(riskScore);
            clue.setRiskScore(riskScore);
            clue.setFinalRiskLevel(finalRiskLevel);

            DisputeClue savedClue = clueRepository.save(clue);
            savedClues.add(savedClue);

            RiskWarning warning = warningService.generateWarning(savedClue);
            if (warning != null) {
                generatedWarnings.add(warning);
                clueRepository.save(savedClue);
            }
        }

        task.setStatus(TaskStatus.已提交);
        task.setSubmittedAt(LocalDateTime.now());
        task.setReportId(savedReport.getId());
        taskRepository.save(task);

        Map<String, Object> response = new HashMap<>();
        response.put("report", savedReport);
        response.put("clues", savedClues);
        response.put("warnings", generatedWarnings);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<InvestigationReport>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long mediatorId,
            @RequestParam(required = false) Long organizationId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<InvestigationReport> result;

        if (mediatorId != null) {
            result = reportRepository.findByMediatorId(mediatorId, pageable);
        } else if (organizationId != null) {
            result = reportRepository.findByOrganizationId(organizationId, pageable);
        } else {
            result = reportRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<InvestigationReport> reportOpt = reportRepository.findById(id);
        if (reportOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InvestigationReport report = reportOpt.get();
        List<DisputeClue> clues = clueRepository.findByReportId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("report", report);
        response.put("clues", clues);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getByTaskId(@PathVariable Long taskId) {
        Optional<InvestigationReport> reportOpt = reportRepository.findByTaskId(taskId);
        if (reportOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InvestigationReport report = reportOpt.get();
        List<DisputeClue> clues = clueRepository.findByReportId(report.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("report", report);
        response.put("clues", clues);

        return ResponseEntity.ok(response);
    }
}
