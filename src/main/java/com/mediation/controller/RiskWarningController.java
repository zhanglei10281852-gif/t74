package com.mediation.controller;

import com.mediation.entity.DisputeClue;
import com.mediation.entity.DisputeClue.RiskLevel;
import com.mediation.entity.Mediator;
import com.mediation.entity.Mediator.MediatorStatus;
import com.mediation.entity.RiskWarning;
import com.mediation.entity.RiskWarning.WarningStatus;
import com.mediation.repository.DisputeClueRepository;
import com.mediation.repository.MediatorRepository;
import com.mediation.repository.RiskWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/risk-warnings")
@RequiredArgsConstructor
public class RiskWarningController {

    private final RiskWarningRepository warningRepository;
    private final DisputeClueRepository clueRepository;
    private final MediatorRepository mediatorRepository;

    @GetMapping
    public ResponseEntity<Page<RiskWarning>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warningLevel,
            @RequestParam(required = false) Long organizationId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<RiskWarning> result;

        if (status != null) {
            WarningStatus ws = WarningStatus.valueOf(status);
            result = warningRepository.findByStatus(ws, pageable);
        } else if (warningLevel != null) {
            RiskLevel wl = RiskLevel.valueOf(warningLevel);
            result = warningRepository.findByWarningLevel(wl, pageable);
        } else if (organizationId != null) {
            result = warningRepository.findByOrganizationId(organizationId, pageable);
        } else {
            result = warningRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<RiskWarning> warningOpt = warningRepository.findById(id);
        if (warningOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RiskWarning warning = warningOpt.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("warning", warning);

        Optional<DisputeClue> clueOpt = clueRepository.findById(warning.getClueId());
        clueOpt.ifPresent(clue -> response.put("clue", clue));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/view")
    public ResponseEntity<?> viewWarning(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String viewedBy = body.get("viewedBy");
        if (viewedBy == null || viewedBy.isBlank()) {
            viewedBy = "管理员";
        }

        Optional<RiskWarning> warningOpt = warningRepository.findById(id);
        if (warningOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RiskWarning warning = warningOpt.get();
        if (warning.getStatus() != WarningStatus.已发出) {
            return ResponseEntity.badRequest().body(Map.of("error", "只有已发出状态的预警才能标记为已关注"));
        }

        warning.setStatus(WarningStatus.已关注);
        warning.setViewedAt(LocalDateTime.now());
        warning.setViewedBy(viewedBy);
        RiskWarning saved = warningRepository.save(warning);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/intervene")
    public ResponseEntity<?> interveneWarning(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long mediatorId = body.get("mediatorId") != null ? Long.valueOf(body.get("mediatorId").toString()) : null;
        String intervenedBy = body.get("intervenedBy") != null ? body.get("intervenedBy").toString() : "管理员";

        Optional<RiskWarning> warningOpt = warningRepository.findById(id);
        if (warningOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RiskWarning warning = warningOpt.get();
        if (warning.getStatus() != WarningStatus.已发出 && warning.getStatus() != WarningStatus.已关注) {
            return ResponseEntity.badRequest().body(Map.of("error", "只有已发出或已关注状态的预警才能介入"));
        }

        if (mediatorId != null) {
            Optional<Mediator> mediatorOpt = mediatorRepository.findById(mediatorId);
            if (mediatorOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "调解员不存在"));
            }
            Mediator mediator = mediatorOpt.get();
            if (mediator.getStatus() != MediatorStatus.在岗) {
                return ResponseEntity.badRequest().body(Map.of("error", "该调解员当前不在岗"));
            }
            warning.setMediatorId(mediator.getId());
            warning.setMediatorName(mediator.getName());
        }

        warning.setStatus(WarningStatus.已介入);
        warning.setIntervenedAt(LocalDateTime.now());
        warning.setIntervenedBy(intervenedBy);
        RiskWarning saved = warningRepository.save(warning);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveWarning(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String remark = body.get("remark");

        Optional<RiskWarning> warningOpt = warningRepository.findById(id);
        if (warningOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RiskWarning warning = warningOpt.get();
        if (warning.getStatus() != WarningStatus.已介入) {
            return ResponseEntity.badRequest().body(Map.of("error", "只有已介入状态的预警才能标记为已化解"));
        }

        warning.setStatus(WarningStatus.已化解);
        warning.setResolvedAt(LocalDateTime.now());
        warning.setRemark(remark);
        RiskWarning saved = warningRepository.save(warning);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/escalate")
    public ResponseEntity<?> escalateWarning(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String remark = body.get("remark");

        Optional<RiskWarning> warningOpt = warningRepository.findById(id);
        if (warningOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RiskWarning warning = warningOpt.get();
        if (warning.getStatus() != WarningStatus.已介入) {
            return ResponseEntity.badRequest().body(Map.of("error", "只有已介入状态的预警才能升级"));
        }

        warning.setStatus(WarningStatus.已升级);
        warning.setEscalatedAt(LocalDateTime.now());
        warning.setRemark(remark);
        RiskWarning saved = warningRepository.save(warning);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<?> statsOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = warningRepository.count();
        stats.put("total", total);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (WarningStatus status : WarningStatus.values()) {
            byStatus.put(status.name(), warningRepository.countByStatus(status));
        }
        stats.put("byStatus", byStatus);

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            byLevel.put(level.name(), warningRepository.countByWarningLevel(level));
        }
        stats.put("byLevel", byLevel);

        return ResponseEntity.ok(stats);
    }
}
