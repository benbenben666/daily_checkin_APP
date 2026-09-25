package com.ben.daily_check_in.controller;

import com.ben.daily_check_in.common.Result;
import com.ben.daily_check_in.dto.task.*;
import com.ben.daily_check_in.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务：发布 / 编辑 / 列表 / 详情 / 完成 / 取消。
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/companies/{id}/tasks")
    public Result<Long> create(@PathVariable("id") Long id, @RequestBody CreateTaskRequest req) {
        return Result.ok(taskService.create(id, req));
    }

    /** 任务列表。view=history 时员工看历史视图，否则看待抢视图；管理者看全部 */
    @GetMapping("/companies/{id}/tasks")
    public Result<List<TaskResponse>> list(@PathVariable("id") Long id,
                                           @RequestParam(value = "view", required = false) String view) {
        return Result.ok(taskService.list(id, view));
    }

    @GetMapping("/tasks/{id}")
    public Result<TaskResponse> detail(@PathVariable("id") Long id) {
        return Result.ok(taskService.detail(id));
    }

    @PutMapping("/tasks/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody UpdateTaskRequest req) {
        taskService.update(id, req);
        return Result.ok();
    }

    @PostMapping("/tasks/{id}/complete")
    public Result<Void> complete(@PathVariable("id") Long id,
                                 @RequestBody(required = false) CompleteTaskRequest req) {
        taskService.complete(id, req);
        return Result.ok();
    }

    @PostMapping("/tasks/{id}/cancel")
    public Result<Void> cancel(@PathVariable("id") Long id,
                               @RequestBody(required = false) CancelTaskRequest req) {
        taskService.cancel(id, req == null ? null : req.getCancelReason());
        return Result.ok();
    }
}
