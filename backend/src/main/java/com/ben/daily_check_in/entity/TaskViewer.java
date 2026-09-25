package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务可见人白名单，对应表 task_viewer。
 * 仅当 task.visibility = 'RESTRICTED' 时生效。
 */
@Data
public class TaskViewer {

    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime createdAt;
}
