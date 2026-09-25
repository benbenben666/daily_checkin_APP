package com.ben.daily_check_in.dto.auth;

import lombok.Data;

/**
 * 当前用户信息（GET /api/auth/me）。
 */
@Data
public class MeResponse {

    private Long userId;
    private String phone;
    private String nickname;
    private String avatarKey;
    /** USER / ADMIN */
    private String systemRole;
}
