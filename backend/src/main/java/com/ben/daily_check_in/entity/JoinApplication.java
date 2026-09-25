package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 加入申请，对应表 join_application。
 */
@Data
public class JoinApplication {

    private Long id;
    private Long userId;
    private Long companyId;
    /** MANAGER / EMPLOYEE（不可申请 FOUNDER） */
    private String applyRole;
    /** PENDING / APPROVED / REJECTED / CANCELLED */
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
