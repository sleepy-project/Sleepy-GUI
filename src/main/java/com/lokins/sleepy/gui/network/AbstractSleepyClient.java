package com.lokins.sleepy.gui.network;

import okhttp3.OkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

public abstract class AbstractSleepyClient implements ISleepyClient {
    protected final String baseUrl;
    protected final String secret;
    protected final String deviceName;
    protected final String deviceId;

    // 共享一个 OkHttpClient 实例，提高性能
    protected static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    protected final ObjectMapper mapper = new ObjectMapper();

    public AbstractSleepyClient(String url, String secret, String deviceName) {
        this.baseUrl = url.endsWith("/") ? url : url + "/";
        this.secret = secret;
        this.deviceName = deviceName;
        // 统一 ID 生成规则
        this.deviceId = deviceName.toLowerCase().replaceAll("\\s+", "-");
    }

    @Override
    public abstract boolean connect();

    @Override
    public void disconnect() {}
}