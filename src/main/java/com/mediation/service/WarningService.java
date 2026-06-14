package com.mediation.service;

import com.mediation.entity.DisputeClue;
import com.mediation.entity.DisputeClue.RiskLevel;
import com.mediation.entity.RiskWarning;
import com.mediation.entity.RiskWarning.WarningStatus;
import com.mediation.repository.RiskWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class WarningService {

    private final RiskWarningRepository riskWarningRepository;

    @Transactional
    public RiskWarning generateWarning(DisputeClue clue) {
        if (clue.getFinalRiskLevel() != RiskLevel.高 && clue.getFinalRiskLevel() != RiskLevel.极高) {
            return null;
        }

        RiskWarning existingWarning = riskWarningRepository.findByClueId(clue.getId()).orElse(null);
        if (existingWarning != null) {
            return existingWarning;
        }

        RiskWarning warning = RiskWarning.builder()
                .warningNo(generateWarningNo())
                .clueId(clue.getId())
                .clueDescription(clue.getDescription())
                .warningLevel(clue.getFinalRiskLevel())
                .riskScore(clue.getRiskScore())
                .organizationId(clue.getOrganizationId())
                .organizationName(clue.getOrganizationName())
                .areaName(clue.getAreaName())
                .status(WarningStatus.已发出)
                .build();

        RiskWarning saved = riskWarningRepository.save(warning);
        clue.setWarningId(saved.getId());
        return saved;
    }

    private String generateWarningNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "W" + date + random;
    }
}
