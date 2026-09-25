package com.ben.daily_check_in.controller;

import com.ben.daily_check_in.common.Result;
import com.ben.daily_check_in.dto.company.*;
import com.ben.daily_check_in.service.CompanyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公司：创建 / 我的公司 / 详情 / 申请加入 / 成员 / 拉黑 / 解散。
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public Result<Long> create(@RequestBody CreateCompanyRequest req) {
        return Result.ok(companyService.createCompany(req));
    }

    @GetMapping("/mine")
    public Result<List<MyCompanyResponse>> mine() {
        return Result.ok(companyService.myCompanies());
    }

    @GetMapping("/{id}")
    public Result<CompanyDetailResponse> detail(@PathVariable("id") Long id) {
        return Result.ok(companyService.detail(id));
    }

    @PostMapping("/join")
    public Result<Long> join(@RequestBody JoinCompanyRequest req) {
        return Result.ok(companyService.join(req));
    }

    @GetMapping("/{id}/members")
    public Result<List<MemberResponse>> members(@PathVariable("id") Long id) {
        return Result.ok(companyService.members(id));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        companyService.removeMember(id, userId);
        return Result.ok();
    }

    @PostMapping("/{id}/blacklist")
    public Result<Void> blacklist(@PathVariable("id") Long id, @RequestBody BlacklistRequest req) {
        companyService.blacklist(id, req);
        return Result.ok();
    }

    @PostMapping("/{id}/dissolve")
    public Result<Void> dissolve(@PathVariable("id") Long id) {
        companyService.dissolve(id);
        return Result.ok();
    }

    /** M-13 主动退出公司（离职方式 1，与"被移除/被拉黑"可区分） */
    @PostMapping("/{id}/leave")
    public Result<Void> leave(@PathVariable("id") Long id) {
        companyService.leave(id);
        return Result.ok();
    }
}
