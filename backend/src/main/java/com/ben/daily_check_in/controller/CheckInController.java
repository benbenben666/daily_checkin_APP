package com.ben.daily_check_in.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 签到相关的 HTTP 接口
 */
@RestController
@RequestMapping("/checkin")
public class CheckInController {

    /**
     * 跑通验证用：启动项目后浏览器访问 http://localhost:8080/checkin/hello
     * 能返回内容就说明项目跑起来了，这个方法之后可以删掉
     */
    @GetMapping("/hello")
    public String hello() {
        return "checkin ok";
    }
}