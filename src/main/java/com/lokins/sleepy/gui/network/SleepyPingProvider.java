package com.lokins.sleepy.gui.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

public class SleepyPingProvider {
    private static final Logger logger = LoggerFactory.getLogger(SleepyPingProvider.class);

    private static final OkHttpClient pingClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();

    public static PingResult testConnection(String url) {
        if (url == null || url.isBlank()) {
            return PingResult.fail("URL 不能为空");
        }

        // 自动补全协议头
        String targetUrl = url;
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            targetUrl = "http://" + targetUrl;
        }
        targetUrl = targetUrl.endsWith("/") ? targetUrl : targetUrl + "/";

        Request request = new Request.Builder()
                .url(targetUrl)
                .get()
                .build();

        long start = System.currentTimeMillis();
        try (Response response = pingClient.newCall(request).execute()) {
            long latency = System.currentTimeMillis() - start;
            if (response.isSuccessful()) {
                logger.info("Ping 成功: {} ({}ms)", targetUrl, latency);
                return PingResult.success(latency);
            } else {
                return PingResult.fail("服务器响应错误: " + response.code());
            }
        } catch (Exception e) {
            logger.error("Ping 异常: {}", e.getMessage());
            return PingResult.fail("无法连接服务器，请检查地址或网络");
        }
    }

    public static class PingResult {
        public boolean success;
        public String message;
        public long latencyMs;
        public String serverVersion = "Unknown";

        private PingResult(boolean success, long latencyMs, String message) {
            this.success = success;
            this.latencyMs = latencyMs;
            this.message = message;
        }

        public static PingResult success(long latencyMs) {
            return new PingResult(true, latencyMs, "连接成功");
        }

        public static PingResult fail(String message) {
            return new PingResult(false, 0, message);
        }
    }
}