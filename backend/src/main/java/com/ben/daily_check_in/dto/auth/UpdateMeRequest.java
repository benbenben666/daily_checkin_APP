package com.ben.daily_check_in.dto.auth;

import lombok.Data;

/**
 * 修改个人信息请求（仅能改自己）。
 */
@Data
public class UpdateMeRequest {

    /** 新昵称，可空表示不改 */
    private String nickname;
}
