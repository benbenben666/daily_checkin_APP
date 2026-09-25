package com.ben.daily_check_in.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员视角的公司列表项。
 */
@Data
public class AdminCompanyResponse {

    private Long id;
    private String name;
    private String inviteCode;
    /** 1-正常 0-已解散 */
    private Integer status;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime dissolvedAt;
}
