package com.mediation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InvestigationReportDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "排查日期不能为空")
    private LocalDate investigationDate;

    @NotBlank(message = "排查区域不能为空")
    private String investigationArea;

    @NotBlank(message = "排查方式不能为空")
    private String investigationMethod;

    @NotNull(message = "是否发现线索不能为空")
    private Boolean hasClue;

    @Valid
    private List<DisputeClueDTO> clues;

    private String remark;
}
