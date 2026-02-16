package com.lokins.sleepy.gui.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepyClientV5 implements ISleepyClient {
    private static final Logger logger = LoggerFactory.getLogger(SleepyClientV5.class);
    private final String baseUrl;
    private final String secret;
    private final String deviceName;
    private final String deviceId;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public SleepyClientV5(String url, String secret, String deviceName) {
        this.baseUrl = url.endsWith("/") ? url : url + "/";
        this.secret = secret;
        this.deviceName = deviceName;
        this.deviceId = deviceName.toLowerCase().replaceAll("\\s+", "-");
    }

    @Override
    public boolean connect() {
        Request request = new Request.Builder().url(baseUrl).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void sendReport(String appName, boolean isUsing) {
        try {
            ReportPayload payload = new ReportPayload(deviceId, deviceName, appName);
            payload.setUsing(isUsing);

            String json = mapper.writeValueAsString(payload);

            // v5 传统的 URL 参数鉴权
            HttpUrl url = HttpUrl.parse(baseUrl + "api/device/set")
                    .newBuilder()
                    .addQueryParameter("secret", secret)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json, MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    logger.error("v5 上报失败: {}", e.getMessage());
                }
                @Override
                public void onResponse(Call call, Response response) {
                    response.close();
                }
            });
        } catch (Exception e) {
            logger.error("v5 构建请求失败: {}", e.getMessage());
        }
    }

    @Override
    public void sendReport(String status, boolean isUsing, java.util.Map<String, Object> additionalFields) {
        // v5 协议不支持附加字段，我们直接复用基础版逻辑即可
        sendReport(status, isUsing);
    }

    @Override
    public void disconnect() { /* v5 无状态，无需断开 */ }

    @Override
    public boolean isAlive() { return true; }

    @Override
    public String getVersionTag() { return "v5 (Legacy)"; }

    public static int probeProtocolVersion(String baseUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        try {
            // 假设 v6 路径不同
            Request request = new Request.Builder()
                    .url(baseUrl.endsWith("/") ? baseUrl + "api/version" : baseUrl + "/api/version")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String versionInfo = response.body().string();
                    return versionInfo.contains("v6") ? 6 : 5;
                }
            }
        } catch (Exception e) {
            // 探测失败默认返回 5
        }
        return 5;
    }

    public boolean ping() {
        return connect(); // v5 的 ping 其实就是 connect 测试
    }
}