package com.mediation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DisputeClueDTO {

    @NotBlank(message = "线索描述不能为空")
    private String description;

    @NotNull(message = "涉及人数不能为空")
    private Integer involvedPeople;

    private BigDecimal involvedAmount;

    private Boolean hasAmount;

    @NotBlank(message = "纠纷类型不能为空")
    private String disputeType;

    private Boolean hasGroupProtest;

    private String initialRiskLevel;

    private String areaName;
}
