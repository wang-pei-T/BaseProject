package com.baseproject.controller.system.common;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import io.minio.MinioClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class HealthController {

    private final StringRedisTemplate stringRedisTemplate;
    private final MinioClient minioClient;

    public HealthController(StringRedisTemplate stringRedisTemplate, MinioClient minioClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.minioClient = minioClient;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("ok", true);
        result.put("service", "backend");
        result.put("timestamp", Instant.now().toString());
        return ApiResponse.success(result, RequestIdHolder.get());
    }

    @GetMapping("/api/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("message", "pong");
        return ApiResponse.success(result, RequestIdHolder.get());
    }

    @GetMapping("/health/deps")
    public ApiResponse<Map<String, Object>> deps() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("redis", checkRedis());
        result.put("minio", checkMinio());
        return ApiResponse.success(result, RequestIdHolder.get());
    }

    private boolean checkRedis() {
        try {
            String pong = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean checkMinio() {
        try {
            minioClient.listBuckets();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
