package com.ben.daily_check_in.controller;

import com.ben.daily_check_in.common.Result;
import com.ben.daily_check_in.dto.application.ApplicationResponse;
import com.ben.daily_check_in.dto.application.RejectRequest;
import com.ben.daily_check_in.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 申请与审批。
 */
@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /** 某公司的待审批列表 */
    @GetMapping("/companies/{id}/applications")
    public Result<List<ApplicationResponse>> pending(@PathVariable("id") Long id) {
        return Result.ok(applicationService.pending(id));
    }

    /** 某公司的全部申请历史 */
    @GetMapping("/companies/{id}/applications/history")
    public Result<List<ApplicationResponse>> history(@PathVariable("id") Long id) {
        return Result.ok(applicationService.history(id));
    }

    /** 我提交的申请 */
    @GetMapping("/applications/mine")
    public Result<List<ApplicationResponse>> mine() {
        return Result.ok(applicationService.mine());
    }

    @PostMapping("/applications/{id}/approve")
    public Result<Void> approve(@PathVariable("id") Long id) {
        applicationService.approve(id);
        return Result.ok();
    }

    @PostMapping("/applications/{id}/reject")
    public Result<Void> reject(@PathVariable("id") Long id, @RequestBody(required = false) RejectRequest req) {
        applicationService.reject(id, req == null ? null : req.getRejectReason());
        return Result.ok();
    }

    @PostMapping("/applications/{id}/cancel")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        applicationService.cancel(id);
        return Result.ok();
    }
}
