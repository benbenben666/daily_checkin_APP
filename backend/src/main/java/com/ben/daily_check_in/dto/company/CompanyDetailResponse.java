package com.ben.daily_check_in.dto.company;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司详情。
 */
@Data
public class CompanyDetailResponse {

    private Long id;
    private String name;
    /** 仅创始人/管理者可见时返回 */
    private String inviteCode;
    private Integer status;
    private Integer memberCount;
    private LocalDateTime createdAt;
    /** 当前用户在该公司的角色（非成员为 null） */
    private String myRole;
}
