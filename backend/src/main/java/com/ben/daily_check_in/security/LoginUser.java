package com.ben.daily_check_in.security;

/**
 * 当前请求的登录用户快照，存放在 {@link UserContext} 中。
 */
public record LoginUser(Long userId, String phone, String nickname, String systemRole) {

    public boolean isAdmin() {
        return "ADMIN".equals(systemRole);
    }
}
