package com.ben.daily_check_in.dto.admin;

import lombok.Data;

/**
 * 修改用户信息请求。
 */
@Data
public class AdminUpdateUserRequest {

    /** 昵称，可空表示不改 */
    private String nickname;
    /** 1-正常 0-禁用，可空表示不改 */
    private Integer status;
}
