package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司，对应表 company。
 * 不含 founder_id —— 创始人由 user_company 中 company_role='FOUNDER' 表达。
 */
@Data
public class Company {

    private Long id;
    /** 名称，允许重名 */
    private String name;
    /** 邀请码，全局唯一，加入公司的唯一凭据 */
    private String inviteCode;
    /** 1-正常 0-已解散 */
    private Integer status;
    private Long dissolvedBy;
    private LocalDateTime dissolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
