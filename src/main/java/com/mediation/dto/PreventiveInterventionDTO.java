package com.mediation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PreventiveInterventionDTO {

    @NotNull(message = "预警ID不能为空")
    private Long warningId;

    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;

    private String applicantPhone;

    private String applicantIdCard;

    private String respondentName;

    private String respondentPhone;

    private String description;

    private BigDecimal amount;
}
