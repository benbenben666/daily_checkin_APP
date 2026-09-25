package com.ben.daily_check_in.dto.task;

import lombok.Data;

/**
 * 完成任务 / 超时补交请求。文字与图片均可为空（允许什么都不填直接提交）。
 */
@Data
public class CompleteTaskRequest {

    /** 提交文字，可空 */
    private String submitContent;
    /** 提交图片的 OSS object key 列表，可空 */
    private java.util.List<String> submitImages;
}
