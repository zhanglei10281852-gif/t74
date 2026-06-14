package com.mediation.service;

import com.mediation.entity.Dispute;
import com.mediation.entity.DisputeClue;
import com.mediation.entity.DisputeClue.RiskLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RiskAssessmentService {

    public int calculateRiskScore(DisputeClue clue) {
        int baseScore = 0;

        int peopleScore = getPeopleScore(clue.getInvolvedPeople());
        int amountScore = getAmountScore(clue.getHasAmount(), clue.getInvolvedAmount());
        int protestScore = getProtestScore(clue.getHasGroupProtest());

        baseScore = peopleScore + amountScore + protestScore;

        double typeCoefficient = getTypeCoefficient(clue.getDisputeType());

        return (int) Math.round(baseScore * typeCoefficient);
    }

    public RiskLevel determineRiskLevel(int riskScore) {
        if (riskScore > 60) {
            return RiskLevel.极高;
        } else if (riskScore >= 40) {
            return RiskLevel.高;
        } else if (riskScore >= 20) {
            return RiskLevel.中;
        } else {
            return RiskLevel.低;
        }
    }

    private int getPeopleScore(Integer involvedPeople) {
        if (involvedPeople == null) {
            return 5;
        }
        if (involvedPeople > 10) {
            return 20;
        } else if (involvedPeople >= 5) {
            return 10;
        } else {
            return 5;
        }
    }

    private int getAmountScore(Boolean hasAmount, BigDecimal amount) {
        if (hasAmount == null || !hasAmount || amount == null) {
            return 0;
        }
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            return 15;
        } else if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            return 10;
        } else {
            return 5;
        }
    }

    private int getProtestScore(Boolean hasGroupProtest) {
        return Boolean.TRUE.equals(hasGroupProtest) ? 30 : 0;
    }

    private double getTypeCoefficient(Dispute.DisputeType disputeType) {
        if (disputeType == null) {
            return 1.0;
        }
        switch (disputeType) {
            case 土地权属:
                return 1.5;
            case 劳动争议:
                return 1.3;
            case 损害赔偿:
                return 1.2;
            default:
                return 1.0;
        }
    }

    public double getRiskCoefficient(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return 1.0;
        }
        switch (riskLevel) {
            case 极高:
                return 4.0;
            case 高:
                return 3.0;
            case 中:
                return 2.0;
            case 低:
                return 1.0;
            default:
                return 1.0;
        }
    }
}
