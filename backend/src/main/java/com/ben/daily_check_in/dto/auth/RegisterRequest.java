package com.ben.daily_check_in.dto.auth;

import lombok.Data;

/**
 * 注册请求。
 */
@Data
public class RegisterRequest {

    /** 手机号，全局唯一 */
    private String phone;
    /** 明文密码，服务端加密存储 */
    private String password;
    /** 昵称，可空 */
    private String nickname;
}
