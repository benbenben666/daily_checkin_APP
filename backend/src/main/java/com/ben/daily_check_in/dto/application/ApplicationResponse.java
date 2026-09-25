package com.ben.daily_check_in.dto.application;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申请记录（审批列表 / 历史列表共用）。
 */
@Data
public class ApplicationResponse {

    private Long id;
    private Long userId;
    /** 目标公司 ID */
    private Long companyId;
    /** 目标公司名称 */
    private String companyName;
    private String userNickname;
    private String userPhone;
    /** 申请身份：MANAGER / EMPLOYEE */
    private String applyRole;
    /** PENDING / APPROVED / REJECTED / CANCELLED */
    private String status;
    private Long reviewerId;
    private String reviewerNickname;
    private LocalDateTime reviewedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
}
