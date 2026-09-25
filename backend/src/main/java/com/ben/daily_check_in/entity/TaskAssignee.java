package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务指派关系，对应表 task_assignee。
 */
@Data
public class TaskAssignee {

    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime assignedAt;
}
