package com.ben.daily_check_in.controller;

import com.ben.daily_check_in.common.Result;
import com.ben.daily_check_in.service.FileService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 附件：OSS 直传签名。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 获取直传凭证。bizType：detail-任务详情配图 submit-完成提交图。
     * 前端拿到后以 multipart/form-data POST 到 uploadUrl 完成直传。
     */
    @PostMapping("/policy")
    public Result<Map<String, Object>> policy(@RequestParam(value = "bizType", defaultValue = "detail") String bizType) {
        return Result.ok(fileService.policy(bizType));
    }
}
