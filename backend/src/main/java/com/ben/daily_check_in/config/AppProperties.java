package com.ben.daily_check_in.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 中 app.* 配置项的绑定。
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Oss oss = new Oss();

    @Data
    public static class Jwt {
        /** 签名密钥（生产环境必须更换） */
        private String secret;
        /** 令牌有效期（小时） */
        private long expireHours = 168;
    }

    @Data
    public static class Oss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucket;
    }
}
