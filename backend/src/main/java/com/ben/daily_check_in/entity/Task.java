package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务，对应表 task。
 * deadline_at / late_deadline_at / is_late 是数据库生成列，应用层不写入。
 */
@Data
public class Task {

    private Long id;
    private Long companyId;
    private Long publisherId;
    /** GLOBAL / ASSIGNED */
    private String taskType;
    private String title;
    private String description;
    /** 限时时长（分钟），5 ~ 43200 */
    private Integer timeLimitMinutes;
    private LocalDateTime createdAt;
    /** 生成列：完成截止 = 发布时间 + 限时时长 */
    private LocalDateTime deadlineAt;
    /** 生成列：补交截止 = 完成截止 + 限时时长 */
    private LocalDateTime lateDeadlineAt;
    /** PENDING / EXPIRED / COMPLETED / CANCELLED */
    private String status;
    private Long completedBy;
    private LocalDateTime completedAt;
    private String submitContent;
    private Long cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    /** 1-允许超时补交 0-不允许 */
    private Integer allowLateSubmit;
    private LocalDateTime updatedAt;
    /** 生成列：0-按时 1-超时完成 */
    private Integer isLate;
}
