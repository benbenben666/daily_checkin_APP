package com.ben.daily_check_in.dto.company;

import lombok.Data;

/**
 * 拉黑成员请求。
 */
@Data
public class BlacklistRequest {

    /** 被拉黑的用户 ID */
    private Long userId;
    /** 拉黑原因，可空 */
    private String reason;
}
