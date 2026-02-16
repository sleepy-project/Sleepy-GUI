package com.lokins.sleepy.gui.utils;

import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;

    private final File configFile = new File(PathUtils.getDataPath("config.ini"));
    private final Wini ini;

    private ConfigManager() throws IOException {
        // PathUtils.getDataPath 内部已经处理了 mkdirs()，但这里双重检查更稳健
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (!configFile.exists()) {
            configFile.createNewFile();
            logger.info("未检测到配置文件，已在隐藏目录创建: {}", configFile.getAbsolutePath());
        }

        ini = new Wini(configFile);
    }

    public static synchronized ConfigManager getInstance() throws IOException {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public String get(String section, String key, String defaultValue) {
        String value = ini.get(section, key);
        return value != null ? value : defaultValue;
    }

    public void set(String section, String key, String value) {
        ini.put(section, key, value);
    }

    public void save() throws IOException {
        ini.store();
        // 使用 logger 代替 System.out，确保日志文件也能记录保存动作
        logger.info("[Config] 配置已成功保存至: {}", configFile.getAbsolutePath());
    }
}