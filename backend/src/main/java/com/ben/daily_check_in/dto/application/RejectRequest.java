package com.ben.daily_check_in.dto.application;

import lombok.Data;

/**
 * 拒绝申请请求。
 */
@Data
public class RejectRequest {

    /** 拒绝理由，可空 */
    private String rejectReason;
}
