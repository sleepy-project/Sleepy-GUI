package com.lokins.sleepy.gui.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SleepyClientV6 implements ISleepyClient {
    private static final Logger logger = LoggerFactory.getLogger(SleepyClientV6.class);

    private final String baseUrl;
    private final String secret;
    private final String refreshToken; // 配置文件中的 refresh_token
    private final String deviceName;
    private final String deviceId;

    // 运行时获取的 access token
    private String token;

    private MyWebSocketClient wsClient;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public SleepyClientV6(String url, String secret, String refreshToken, String deviceName) {
        this.baseUrl = url.endsWith("/") ? url : url + "/";
        this.secret = secret;
        this.refreshToken = refreshToken;
        this.deviceName = deviceName;
        // 确保 deviceId 与 Python 版逻辑一致，如果服务器对 ID 格式敏感，需确保 config.ini 里的 name 生成的 ID 是对的
        // 建议：如果 config.ini 有 device_id 字段最好直接读取，没有则沿用此逻辑
        this.deviceId = deviceName.toLowerCase().replaceAll("\\s+", "-");

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public boolean connect() {
        try {
            boolean authSuccess = false;

            // 1. 优先尝试刷新 Token (参考 Python 逻辑)
            if (refreshToken != null && !refreshToken.isEmpty()) {
                logger.info("正在尝试使用 RefreshToken 刷新凭证...");
                if (performRefresh()) {
                    authSuccess = true;
                } else {
                    logger.warn("RefreshToken 失效，尝试降级使用 Secret 登录...");
                }
            }

            // 2. 刷新失败或没有 refresh token，尝试使用 secret 登录
            if (!authSuccess) {
                if (performLogin()) {
                    authSuccess = true;
                }
            }

            // 3. 鉴权成功后，建立 WebSocket 长连接
            if (authSuccess) {
                connectWebSocket();
                return true;
            }

        } catch (Exception e) {
            logger.error("V6 连接流程异常: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 建立 WebSocket 连接 (核心修复点)
     */
    private void connectWebSocket() throws Exception {
        if (wsClient != null && wsClient.isOpen()) {
            return;
        }

        // 拼接 WebSocket URL: ws://host/api/devices/{device_id}
        String wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
                + "api/devices/" + deviceId;

        logger.info("正在建立 WebSocket 连接: {}", wsUrl);

        // 准备请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Sleepy-Token", this.token); // 关键：Python 版的鉴权头

        wsClient = new MyWebSocketClient(new URI(wsUrl), headers);

        // 阻塞式连接，确保 connect() 返回 true 时连接已就绪
        boolean connected = wsClient.connectBlocking(5, TimeUnit.SECONDS);
        if (!connected) {
            throw new RuntimeException("WebSocket 连接超时");
        }
    }

    /**
     * 实现真正的 Token 刷新
     */
    private boolean performRefresh() {
        try {
            Map<String, String> payload = new HashMap<>();

            payload.put("token", this.token == null ? "" : this.token);
            payload.put("refresh_token", this.refreshToken);

            RequestBody body = RequestBody.create(
                    mapper.writeValueAsString(payload),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(baseUrl + "api/auth/refresh")
                    .post(body)
                    // Python 刷新时也带了 X-Sleepy-Token，虽然可能过期
                    .addHeader("X-Sleepy-Token", this.token == null ? "" : this.token)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    parseTokenResponse(response.body().string());
                    logger.info("Token 刷新成功");
                    return true;
                } else {
                    logger.warn("Token 刷新失败, 状态码: {}", response.code());
                }
            }
        } catch (Exception e) {
            logger.error("刷新 Token 请求异常: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 实现登录逻辑
     */
    private boolean performLogin() {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("secret", secret);

            RequestBody body = RequestBody.create(
                    mapper.writeValueAsString(payload),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(baseUrl + "api/auth/login")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    parseTokenResponse(response.body().string());
                    logger.info("Secret 登录成功");
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("登录请求异常: {}", e.getMessage());
        }
        return false;
    }

    private void parseTokenResponse(String json) throws Exception {
        Map<String, Object> res = mapper.readValue(json,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

        if (res.containsKey("token")) {
            this.token = (String) res.get("token");
            // 注意：这里没有回写 refresh_token 到文件，
            // 如果服务器返回了新的 refresh_token，理论上应该更新 ConfigManager 并保存
        }
    }

    @Override
    public void disconnect() {
        if (wsClient != null) {
            wsClient.close();
            wsClient = null;
        }
    }

    @Override
    public void sendReport(String appName, boolean isUsing) {
        // 复用带 Map 的逻辑，构建基础 fields
        Map<String, Object> fields = new HashMap<>();
        fields.put("window", appName); // 模仿 Python 把窗口名也放入 fields
        sendReport(appName, isUsing, fields);
    }

    @Override
    public void sendReport(String status, boolean isUsing, Map<String, Object> additionalFields) {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                // 构造与 Python 结构兼容的 Payload
                ReportPayload payload = new ReportPayload(deviceId, deviceName, status);
                payload.setUsing(isUsing);

                // 将附加字段填入
                if (additionalFields != null) {
                    payload.getFields().putAll(additionalFields);
                }

                String json = mapper.writeValueAsString(payload);
                wsClient.send(json);
                // 降低日志级别，避免刷屏
                logger.debug("V6 上报成功: {}", status);
            } catch (Exception e) {
                logger.error("V6 上报序列化异常: {}", e.getMessage());
            }
        } else {
            // 如果断连，尝试重连（可选优化）
            logger.warn("V6 上报失败：WebSocket 未连接");
        }
    }

    @Override
    public boolean ping() {
        return isAlive();
    }

    @Override
    public boolean isAlive() {
        return wsClient != null && wsClient.isOpen();
    }

    @Override public long getLatency() { return -1; }
    @Override public String getVersionTag() { return "v6 (Modern)"; }

    // ---------------- WebSocket 内部类 ----------------
    private class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverUri, Map<String, String> httpHeaders) {
            super(serverUri, new org.java_websocket.drafts.Draft_6455(), httpHeaders, 10000);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            logger.info("WebSocket 通道已开启: {}", getURI());
        }

        @Override
        public void onMessage(String message) {
            logger.debug("收到服务器指令: {}", message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            logger.warn("WebSocket 连接断开: {} (Code: {})", reason, code);
        }

        @Override
        public void onError(Exception ex) {
            logger.error("WebSocket 错误: {}", ex.getMessage());
        }
    }
}