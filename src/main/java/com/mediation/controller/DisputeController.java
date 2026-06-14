package com.mediation.controller;

import com.mediation.dto.DisputeDTO;
import com.mediation.dto.PreventiveInterventionDTO;
import com.mediation.entity.Dispute;
import com.mediation.entity.Dispute.DisputeSource;
import com.mediation.entity.Dispute.DisputeStatus;
import com.mediation.entity.Dispute.DisputeType;
import com.mediation.entity.DisputeClue;
import com.mediation.entity.Mediator;
import com.mediation.entity.Mediator.MediatorStatus;
import com.mediation.entity.RiskWarning;
import com.mediation.entity.RiskWarning.WarningStatus;
import com.mediation.repository.DisputeClueRepository;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.MediatorRepository;
import com.mediation.repository.RiskWarningRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeRepository disputeRepository;
    private final MediatorRepository mediatorRepository;
    private final RiskWarningRepository riskWarningRepository;
    private final DisputeClueRepository disputeClueRepository;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DisputeDTO dto) {
        DisputeType disputeType;
        try {
            disputeType = DisputeType.valueOf(dto.getDisputeType());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的纠纷类型"));
        }

        String caseNo = generateCaseNo();

        Dispute dispute = Dispute.builder()
                .caseNo(caseNo)
                .disputeType(disputeType)
                .applicantName(dto.getApplicantName())
                .applicantPhone(dto.getApplicantPhone())
                .applicantIdCard(dto.getApplicantIdCard())
                .respondentName(dto.getRespondentName())
                .respondentPhone(dto.getRespondentPhone())
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .source(DisputeSource.当事人申请)
                .build();

        Dispute saved = disputeRepository.save(dispute);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/preventive-intervention")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> createPreventiveIntervention(@Valid @RequestBody PreventiveInterventionDTO dto) {
        Optional<RiskWarning> warningOpt = riskWarningRepository.findById(dto.getWarningId());
        if (warningOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "预警不存在"));
        }

        RiskWarning warning = warningOpt.get();
        if (warning.getStatus() != WarningStatus.已介入) {
            return ResponseEntity.badRequest().body(Map.of("error", "只有已介入状态的预警才能发起预防性调解"));
        }

        Optional<DisputeClue> clueOpt = disputeClueRepository.findById(warning.getClueId());

        String applicantIdCard = dto.getApplicantIdCard();
        if (applicantIdCard == null || applicantIdCard.isBlank()) {
            applicantIdCard = "000000000000000000";
        }

        String caseNo = generateCaseNo();

        Dispute dispute = Dispute.builder()
                .caseNo(caseNo)
                .disputeType(clueOpt.map(DisputeClue::getDisputeType).orElse(DisputeType.其他))
                .applicantName(dto.getApplicantName())
                .applicantPhone(dto.getApplicantPhone() != null ? dto.getApplicantPhone() : "")
                .applicantIdCard(applicantIdCard)
                .respondentName(dto.getRespondentName() != null ? dto.getRespondentName() : "")
                .respondentPhone(dto.getRespondentPhone())
                .description(dto.getDescription() != null ? dto.getDescription() :
                        (clueOpt.map(DisputeClue::getDescription).orElse("预防性调解案件")))
                .amount(dto.getAmount() != null ? dto.getAmount() : clueOpt.map(DisputeClue::getInvolvedAmount).orElse(null))
                .source(DisputeSource.排查发现)
                .warningId(warning.getId())
                .clueId(warning.getClueId())
                .mediatorId(warning.getMediatorId())
                .status(DisputeStatus.调解中)
                .build();

        if (warning.getMediatorId() != null) {
            Optional<Mediator> mediatorOpt = mediatorRepository.findById(warning.getMediatorId());
            mediatorOpt.ifPresent(mediator -> {
                mediator.setCaseCount(mediator.getCaseCount() + 1);
                mediatorRepository.save(mediator);
            });
        }

        Dispute savedDispute = disputeRepository.save(dispute);

        warning.setDisputeId(savedDispute.getId());
        riskWarningRepository.save(warning);

        if (clueOpt.isPresent()) {
            DisputeClue clue = clueOpt.get();
            clue.setHasDispute(true);
            clue.setDisputeId(savedDispute.getId());
            disputeClueRepository.save(clue);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedDispute);
    }

    @GetMapping
    public ResponseEntity<Page<Dispute>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String disputeType,
            @RequestParam(required = false) Long mediatorId,
            @RequestParam(required = false) String source) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Dispute> result;

        if (status != null) {
            DisputeStatus ds = DisputeStatus.valueOf(status);
            result = disputeRepository.findByStatus(ds, pageable);
        } else if (disputeType != null) {
            DisputeType dt = DisputeType.valueOf(disputeType);
            result = disputeRepository.findByDisputeType(dt, pageable);
        } else if (mediatorId != null) {
            result = disputeRepository.findByMediatorId(mediatorId, pageable);
        } else if (source != null) {
            DisputeSource ds = DisputeSource.valueOf(source);
            result = disputeRepository.findBySource(ds, pageable);
        } else {
            result = disputeRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", dispute.getId());
        response.put("caseNo", dispute.getCaseNo());
        response.put("disputeType", dispute.getDisputeType());
        response.put("applicantName", dispute.getApplicantName());
        response.put("applicantPhone", dispute.getApplicantPhone());
        response.put("applicantIdCard", dispute.getApplicantIdCard());
        response.put("respondentName", dispute.getRespondentName());
        response.put("respondentPhone", dispute.getRespondentPhone());
        response.put("description", dispute.getDescription());
        response.put("amount", dispute.getAmount());
        response.put("mediatorId", dispute.getMediatorId());
        response.put("source", dispute.getSource());
        response.put("warningId", dispute.getWarningId());
        response.put("clueId", dispute.getClueId());
        response.put("status", dispute.getStatus());
        response.put("result", dispute.getResult());
        response.put("createdAt", dispute.getCreatedAt());
        response.put("updatedAt", dispute.getUpdatedAt());

        if (dispute.getMediatorId() != null) {
            mediatorRepository.findById(dispute.getMediatorId())
                    .ifPresent(mediator -> response.put("mediatorName", mediator.getName()));
        }

        if (dispute.getWarningId() != null) {
            riskWarningRepository.findById(dispute.getWarningId())
                    .ifPresent(warning -> response.put("warning", warning));
        }

        if (dispute.getClueId() != null) {
            disputeClueRepository.findById(dispute.getClueId())
                    .ifPresent(clue -> response.put("clue", clue));
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.待受理) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有待受理的案件才能受理"));
        }

        dispute.setStatus(DisputeStatus.已受理);
        disputeRepository.save(dispute);
        return ResponseEntity.ok(dispute);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long mediatorId = body.get("mediatorId");
        if (mediatorId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "调解员ID不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.已受理) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有已受理的案件才能分配调解员"));
        }

        Optional<Mediator> mediatorOpt = mediatorRepository.findById(mediatorId);
        if (mediatorOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "调解员不存在"));
        }

        Mediator mediator = mediatorOpt.get();
        if (mediator.getStatus() != MediatorStatus.在岗) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "该调解员当前不在岗，无法分配"));
        }

        dispute.setMediatorId(mediatorId);
        dispute.setStatus(DisputeStatus.调解中);
        disputeRepository.save(dispute);

        mediator.setCaseCount(mediator.getCaseCount() + 1);
        mediatorRepository.save(mediator);

        return ResponseEntity.ok(dispute);
    }

    @PutMapping("/{id}/close")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> close(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String result = (String) body.get("result");
        Boolean success = (Boolean) body.get("success");

        if (result == null || success == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "结果描述和是否成功不能为空"));
        }

        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.调解中) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有调解中的案件才能结案"));
        }

        dispute.setResult(result);
        dispute.setStatus(success ? DisputeStatus.调解成功 : DisputeStatus.调解失败);
        disputeRepository.save(dispute);

        if (dispute.getWarningId() != null && (dispute.getStatus() == DisputeStatus.调解成功 || dispute.getStatus() == DisputeStatus.已撤回)) {
            Optional<RiskWarning> warningOpt = riskWarningRepository.findById(dispute.getWarningId());
            if (warningOpt.isPresent() && warningOpt.get().getStatus() == WarningStatus.已介入) {
                RiskWarning warning = warningOpt.get();
                warning.setStatus(WarningStatus.已化解);
                warning.setResolvedAt(java.time.LocalDateTime.now());
                warning.setRemark(result);
                riskWarningRepository.save(warning);
            }
        }

        return ResponseEntity.ok(dispute);
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long id) {
        Optional<Dispute> disputeOpt = disputeRepository.findById(id);
        if (disputeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dispute dispute = disputeOpt.get();
        if (dispute.getStatus() != DisputeStatus.待受理 && dispute.getStatus() != DisputeStatus.已受理) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "只有待受理或已受理的案件才能撤回"));
        }

        dispute.setStatus(DisputeStatus.已撤回);
        disputeRepository.save(dispute);

        return ResponseEntity.ok(dispute);
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<?> statsOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = disputeRepository.count();
        stats.put("total", total);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (DisputeStatus status : DisputeStatus.values()) {
            byStatus.put(status.name(), disputeRepository.countByStatus(status));
        }
        stats.put("byStatus", byStatus);

        Map<String, Long> byType = new LinkedHashMap<>();
        for (DisputeType type : DisputeType.values()) {
            byType.put(type.name(), disputeRepository.countByDisputeType(type));
        }
        stats.put("byType", byType);

        Map<String, Long> bySource = new LinkedHashMap<>();
        for (DisputeSource source : DisputeSource.values()) {
            bySource.put(source.name(), disputeRepository.countBySource(source));
        }
        stats.put("bySource", bySource);

        return ResponseEntity.ok(stats);
    }

    private String generateCaseNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "RM" + date + random;
    }
}
