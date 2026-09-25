package com.ben.daily_check_in.dto.company;

import lombok.Data;

/**
 * 创建公司请求。
 */
@Data
public class CreateCompanyRequest {

    /** 公司名称，允许重名 */
    private String name;
}
