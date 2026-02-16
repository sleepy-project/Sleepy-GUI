package com.lokins.sleepy.gui.network;

import com.lokins.sleepy.gui.utils.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepyClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(SleepyClientFactory.class);

    public static ISleepyClient createClient() {
        try {
            ConfigManager config = ConfigManager.getInstance();
            // 读取配置：0=Auto(v6优先), 5=Legacy(v5), 6=Modern(v6)
            String url = config.get("server", "url", "");
            String secret = config.get("auth", "secret", "");
            String refresh = config.get("auth", "refresh_token", ""); // 获取新字段
            String deviceName = config.get("device", "name", "");
            String mode = config.get("settings", "protocol_version", "0");

            if ("5".equals(mode)) {
                logger.info("手动切换到 v5 (Legacy) 模式");
                return new SleepyClientV5(url, secret, deviceName);
            } else if ("6".equals(mode)) {
                logger.info("手动切换到 v6 (Modern/WebSocket) 模式");
                return new SleepyClientV6(url, secret, refresh, deviceName);
            } else {
                // 自动模式逻辑：尝试 v6，失败则降级到 v5
                logger.info("自动选择模式：优先尝试 v6...");
                ISleepyClient v6Client = new SleepyClientV6(url, secret, refresh, deviceName);
                if (v6Client.connect()) {
                    return v6Client;
                }
                logger.warn("v6 连接不可用，自动降级到 v5 模式");
                return new SleepyClientV5(url, secret, deviceName);
            }
        } catch (Exception e) {
            logger.error("创建客户端失败，请检查配置内容", e);
            return null;
        }
    }
}