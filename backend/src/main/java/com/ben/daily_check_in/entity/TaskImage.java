package com.ben.daily_check_in.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务图片，对应表 task_image。
 */
@Data
public class TaskImage {

    private Long id;
    private Long taskId;
    /** DETAIL-详情配图 SUBMIT-完成提交图 */
    private String imageType;
    /** OSS object key，不含域名 */
    private String objectKey;
    private String fileName;
    private Integer fileSize;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
