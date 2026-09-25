package com.ben.daily_check_in.controller;

import com.ben.daily_check_in.common.Result;
import com.ben.daily_check_in.dto.auth.LoginRequest;
import com.ben.daily_check_in.dto.auth.LoginResponse;
import com.ben.daily_check_in.dto.auth.MeResponse;
import com.ben.daily_check_in.dto.auth.RegisterRequest;
import com.ben.daily_check_in.service.AuthService;
import org.springframework.web.bind.annotation.*;

/**
 * 认证：注册 / 登录 / 当前用户。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/me")
    public Result<MeResponse> me() {
        return Result.ok(authService.me());
    }

    @PutMapping("/me")
    public Result<Void> updateMe(@RequestBody com.ben.daily_check_in.dto.auth.UpdateMeRequest req) {
        authService.updateMe(req);
        return Result.ok();
    }
}
