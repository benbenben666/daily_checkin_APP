package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成员关系，对应表 user_company。
 */
@Data
public class UserCompany {

    private Long id;
    private Long userId;
    private Long companyId;
    /** FOUNDER / MANAGER / EMPLOYEE */
    private String companyRole;
    /** 1-在职 0-已离职 */
    private Integer status;
    /** 1-主动退出 2-被移除 3-被拉黑 */
    private Integer leaveType;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private LocalDateTime updatedAt;
}
