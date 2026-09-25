package com.ben.daily_check_in.dto.company;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我所在的公司及我在其中的角色。
 */
@Data
public class MyCompanyResponse {

    private Long companyId;
    private String companyName;
    /** 我在该公司的角色：FOUNDER / MANAGER / EMPLOYEE */
    private String companyRole;
    private LocalDateTime joinedAt;
}
