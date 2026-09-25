package com.ben.daily_check_in.controller;

import com.ben.daily_check_in.common.Result;
import com.ben.daily_check_in.dto.admin.*;
import com.ben.daily_check_in.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理（仅 ADMIN）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Result<List<AdminUserResponse>> users(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.ok(adminService.listUsers(keyword));
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable("id") Long id, @RequestBody AdminUpdateUserRequest req) {
        adminService.updateUser(id, req);
        return Result.ok();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable("id") Long id) {
        adminService.deleteUser(id);
        return Result.ok();
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> updateRole(@PathVariable("id") Long id, @RequestBody AdminRoleRequest req) {
        adminService.updateRole(id, req == null ? null : req.getSystemRole());
        return Result.ok();
    }

    @GetMapping("/companies")
    public Result<List<AdminCompanyResponse>> companies(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.ok(adminService.listCompanies(keyword));
    }

    @PostMapping("/companies/{id}/dissolve")
    public Result<Void> dissolveCompany(@PathVariable("id") Long id) {
        adminService.dissolveCompany(id);
        return Result.ok();
    }
}
