package com.mediation.controller;

import com.mediation.entity.DisputeClue;
import com.mediation.entity.DisputeClue.RiskLevel;
import com.mediation.entity.RiskWarning;
import com.mediation.repository.DisputeClueRepository;
import com.mediation.repository.DisputeRepository;
import com.mediation.repository.InvestigationReportRepository;
import com.mediation.repository.InvestigationTaskRepository;
import com.mediation.repository.RiskWarningRepository;
import com.mediation.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final InvestigationTaskRepository taskRepository;
    private final InvestigationReportRepository reportRepository;
    private final DisputeClueRepository clueRepository;
    private final RiskWarningRepository warningRepository;
    private final DisputeRepository disputeRepository;
    private final RiskAssessmentService riskAssessmentService;

    @GetMapping("/monthly-coverage")
    public ResponseEntity<?> getMonthlyCoverage(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") String month) {

        String taskMonth = month != null ? month : YearMonth.now().toString();

        long totalTasks = taskRepository.countByTaskMonth(taskMonth);
        long submittedTasks = taskRepository.countSubmittedByTaskMonth(taskMonth);

        double coverageRate = totalTasks > 0
                ? BigDecimal.valueOf(submittedTasks)
                        .divide(BigDecimal.valueOf(totalTasks), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", taskMonth);
        result.put("totalTasks", totalTasks);
        result.put("submittedTasks", submittedTasks);
        result.put("coverageRate", coverageRate);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/clue-discovery-rate")
    public ResponseEntity<?> getClueDiscoveryRate(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<?> reports = reportRepository.findByDateRange(start, end);
        long totalReports = reports.size();
        long reportsWithClues = reportRepository.countReportsWithClues(start, end);
        long totalClues = clueRepository.countByCreatedAtBetween(start.atStartOfDay(), end.atTime(23, 59, 59));

        double discoveryRate = totalReports > 0
                ? BigDecimal.valueOf(reportsWithClues)
                        .divide(BigDecimal.valueOf(totalReports), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        double avgCluesPerReport = totalReports > 0
                ? BigDecimal.valueOf(totalClues)
                        .divide(BigDecimal.valueOf(totalReports), 2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start);
        result.put("endDate", end);
        result.put("totalReports", totalReports);
        result.put("reportsWithClues", reportsWithClues);
        result.put("totalClues", totalClues);
        result.put("discoveryRate", discoveryRate);
        result.put("avgCluesPerReport", avgCluesPerReport);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/risk-distribution")
    public ResponseEntity<?> getRiskDistribution(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        LocalDateTime start = (startDate != null ? startDate : LocalDate.now().withDayOfMonth(1)).atStartOfDay();
        LocalDateTime end = (endDate != null ? endDate : LocalDate.now()).atTime(23, 59, 59);

        List<DisputeClue> clues = clueRepository.findByCreatedAtBetween(start, end);

        Map<String, Long> distribution = new LinkedHashMap<>();
        Map<String, Double> percentage = new LinkedHashMap<>();
        long total = clues.size();

        for (RiskLevel level : RiskLevel.values()) {
            long count = clues.stream()
                    .filter(c -> level.equals(c.getFinalRiskLevel()))
                    .count();
            distribution.put(level.name(), count);
            double pct = total > 0
                    ? BigDecimal.valueOf(count)
                            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                    : 0.0;
            percentage.put(level.name(), pct);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start.toLocalDate());
        result.put("endDate", end.toLocalDate());
        result.put("total", total);
        result.put("distribution", distribution);
        result.put("percentage", percentage);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/warning-response-timeliness")
    public ResponseEntity<?> getWarningResponseTimeliness(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        LocalDateTime start = (startDate != null ? startDate : LocalDate.now().withDayOfMonth(1)).atStartOfDay();
        LocalDateTime end = (endDate != null ? endDate : LocalDate.now()).atTime(23, 59, 59);

        List<RiskWarning> warnings = warningRepository.findByIssuedAtBetween(start, end);

        long totalWarnings = warnings.size();
        long intervenedCount = warnings.stream()
                .filter(w -> w.getIntervenedAt() != null)
                .count();

        List<Long> durations = new ArrayList<>();
        for (RiskWarning w : warnings) {
            if (w.getIntervenedAt() != null && w.getIssuedAt() != null) {
                long hours = Duration.between(w.getIssuedAt(), w.getIntervenedAt()).toHours();
                durations.add(hours);
            }
        }

        double avgHours = durations.isEmpty() ? 0.0 :
                durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double avgDays = avgHours / 24.0;

        double timelinessRate = totalWarnings > 0
                ? BigDecimal.valueOf(intervenedCount)
                        .divide(BigDecimal.valueOf(totalWarnings), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start.toLocalDate());
        result.put("endDate", end.toLocalDate());
        result.put("totalWarnings", totalWarnings);
        result.put("intervenedCount", intervenedCount);
        result.put("timelinessRate", timelinessRate);
        result.put("avgResponseHours", Math.round(avgHours * 100.0) / 100.0);
        result.put("avgResponseDays", Math.round(avgDays * 100.0) / 100.0);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/preventive-intervention-success-rate")
    public ResponseEntity<?> getPreventiveInterventionSuccessRate() {
        long total = disputeRepository.countTotalPreventiveMediation();
        long successful = disputeRepository.countSuccessfulPreventiveMediation();

        double successRate = total > 0
                ? BigDecimal.valueOf(successful)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPreventiveMediation", total);
        result.put("successfulPreventiveMediation", successful);
        result.put("successRate", successRate);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/area-heat-rank")
    public ResponseEntity<?> getAreaHeatRank(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        LocalDateTime start = (startDate != null ? startDate : LocalDate.now().withDayOfMonth(1)).atStartOfDay();
        LocalDateTime end = (endDate != null ? endDate : LocalDate.now()).atTime(23, 59, 59);

        List<DisputeClue> clues = clueRepository.findByCreatedAtBetween(start, end);

        Map<String, long[]> areaStats = new LinkedHashMap<>();
        for (DisputeClue clue : clues) {
            String area = clue.getAreaName();
            if (!areaStats.containsKey(area)) {
                areaStats.put(area, new long[]{0, 0});
            }
            long[] stats = areaStats.get(area);
            stats[0]++;
            double coeff = riskAssessmentService.getRiskCoefficient(clue.getFinalRiskLevel());
            stats[1] += coeff;
        }

        List<Map<String, Object>> rankList = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : areaStats.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("areaName", entry.getKey());
            item.put("clueCount", entry.getValue()[0]);
            item.put("heatScore", entry.getValue()[1]);
            rankList.add(item);
        }

        rankList.sort((a, b) -> Long.compare((Long) b.get("heatScore"), (Long) a.get("heatScore")));

        if (rankList.size() > limit) {
            rankList = rankList.subList(0, limit);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", start.toLocalDate());
        result.put("endDate", end.toLocalDate());
        result.put("rank", rankList);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        LocalDate start = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        String month = YearMonth.from(start).toString();

        Map<String, Object> overview = new LinkedHashMap<>();

        Map<String, Object> coverage = (Map<String, Object>) getMonthlyCoverage(month).getBody();
        overview.put("monthlyCoverage", coverage);

        Map<String, Object> clueDiscovery = (Map<String, Object>) getClueDiscoveryRate(start, end).getBody();
        overview.put("clueDiscovery", clueDiscovery);

        Map<String, Object> riskDistribution = (Map<String, Object>) getRiskDistribution(start, end).getBody();
        overview.put("riskDistribution", riskDistribution);

        Map<String, Object> timeliness = (Map<String, Object>) getWarningResponseTimeliness(start, end).getBody();
        overview.put("warningTimeliness", timeliness);

        Map<String, Object> preventive = (Map<String, Object>) getPreventiveInterventionSuccessRate().getBody();
        overview.put("preventiveIntervention", preventive);

        return ResponseEntity.ok(overview);
    }
}
