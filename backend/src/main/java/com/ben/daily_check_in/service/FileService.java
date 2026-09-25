package com.ben.daily_check_in.service;

import com.ben.daily_check_in.config.AppProperties;
import com.ben.daily_check_in.security.UserContext;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 附件：OSS 直传签名（F-1/F-2）。
 * 采用 PostObject 策略签名：后端只签发凭证，图片由前端直传 OSS，
 * 数据库只存 object_key，不存完整 URL（见开发文档 6.10）。
 */
@Service
public class FileService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final AppProperties props;

    public FileService(AppProperties props) {
        this.props = props;
    }

    /** 生成直传凭证：objectKey + 上传地址 + 签名参数 */
    public Map<String, Object> policy(String bizType) {
        Long userId = UserContext.requireUserId();
        AppProperties.Oss oss = props.getOss();

        String type = "SUBMIT".equals(bizType) ? "submit" : "detail";
        String date = DATE_FMT.format(Instant.now());
        // object_key 规则：task/{detail|submit}/{日期}/{用户}/{随机}
        String objectKey = "task/" + type + "/" + date + "/" + userId + "/"
                + UUID.randomUUID().toString().replace("-", "");

        String expiration = ISO_FMT.format(Instant.now().plusSeconds(600));
        // PostObject 策略：限定上传目录与大小，Base64 后参与签名
        String policyJson = "{\"expiration\":\"" + expiration + "\","
                + "\"conditions\":[[\"starts-with\",\"$key\",\"task/" + type + "/\"],"
                + "[\"content-length-range\",1,10485760]]}";
        String policyBase64 = Base64.getEncoder()
                .encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha1(oss.getAccessKeySecret(), policyBase64);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectKey", objectKey);
        result.put("uploadUrl", "https://" + oss.getBucket() + "." + oss.getEndpoint());
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", objectKey);
        fields.put("OSSAccessKeyId", oss.getAccessKeyId());
        fields.put("policy", policyBase64);
        fields.put("signature", signature);
        result.put("fields", fields);
        return result;
    }

    private String hmacSha1(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("OSS 签名失败", e);
        }
    }
}
