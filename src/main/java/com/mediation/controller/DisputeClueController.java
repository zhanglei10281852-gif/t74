package com.mediation.controller;

import com.mediation.entity.DisputeClue;
import com.mediation.entity.DisputeClue.RiskLevel;
import com.mediation.repository.DisputeClueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dispute-clues")
@RequiredArgsConstructor
public class DisputeClueController {

    private final DisputeClueRepository clueRepository;

    @GetMapping
    public ResponseEntity<Page<DisputeClue>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String finalRiskLevel,
            @RequestParam(required = false) Long reportId,
            @RequestParam(required = false) Long taskId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DisputeClue> result;

        if (organizationId != null) {
            result = clueRepository.findByOrganizationId(organizationId, pageable);
        } else if (finalRiskLevel != null) {
            RiskLevel rl = RiskLevel.valueOf(finalRiskLevel);
            result = clueRepository.findByFinalRiskLevel(rl, pageable);
        } else {
            result = clueRepository.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<DisputeClue> clueOpt = clueRepository.findById(id);
        return clueOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<?> getByReportId(@PathVariable Long reportId) {
        return ResponseEntity.ok(clueRepository.findByReportId(reportId));
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getByTaskId(@PathVariable Long taskId) {
        return ResponseEntity.ok(clueRepository.findByTaskId(taskId));
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<?> statsOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long total = clueRepository.count();
        stats.put("total", total);

        Map<String, Long> byRiskLevel = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            byRiskLevel.put(level.name(), clueRepository.countByFinalRiskLevel(level));
        }
        stats.put("byRiskLevel", byRiskLevel);

        return ResponseEntity.ok(stats);
    }
}
