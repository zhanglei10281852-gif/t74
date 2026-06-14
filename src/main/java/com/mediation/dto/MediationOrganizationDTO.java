package com.mediation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MediationOrganizationDTO {

    @NotBlank(message = "组织名称不能为空")
    private String name;

    @NotBlank(message = "管辖区域不能为空")
    private String area;

    @NotBlank(message = "区域类型不能为空")
    private String areaType;

    private String director;

    private String phone;
}
