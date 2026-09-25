package com.ben.daily_check_in.task;

import com.ben.daily_check_in.mapper.TaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时标记超时（见开发文档 7.6）：每分钟把已过截止时间的 PENDING 任务置为 EXPIRED。
 * 注意：存在最长一分钟延迟，因此完成逻辑必须同时校验状态，不能依赖本任务的及时性。
 */
@Slf4j
@Component
public class TaskExpireScheduler {

    private final TaskMapper taskMapper;

    public TaskExpireScheduler(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void markExpired() {
        int rows = taskMapper.markExpired();
        if (rows > 0) {
            log.info("定时任务：{} 条任务已标记为超时", rows);
        }
    }
}
