package com.ben.daily_check_in.dto.auth;

import lombok.Data;

/**
 * 登录成功返回。
 */
@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private String phone;
    private String nickname;
    private String avatarKey;
    /** USER / ADMIN，前端据此分流页面 */
    private String systemRole;
}
