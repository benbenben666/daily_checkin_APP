package com.ben.daily_check_in.dto.auth;

import lombok.Data;

/**
 * 登录请求。
 */
@Data
public class LoginRequest {

    private String phone;
    private String password;
}
