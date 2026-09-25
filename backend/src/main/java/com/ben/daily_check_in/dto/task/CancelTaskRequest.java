package com.ben.daily_check_in.dto.task;

import lombok.Data;

/**
 * 取消任务请求。
 */
@Data
public class CancelTaskRequest {

    /** 取消原因，可空 */
    private String cancelReason;
}
