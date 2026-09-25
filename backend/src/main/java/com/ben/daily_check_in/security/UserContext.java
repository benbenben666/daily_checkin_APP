package com.ben.daily_check_in.security;

import com.ben.daily_check_in.common.BizException;

/**
 * 用 ThreadLocal 保存当前请求的登录用户。
 * 拦截器在请求开始时 set，请求结束时 clear。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 获取当前用户 ID，未登录时抛 401 */
    public static Long requireUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("未登录");
        }
        return user.userId();
    }

    /** 获取当前用户，未登录时抛 401 */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("未登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
