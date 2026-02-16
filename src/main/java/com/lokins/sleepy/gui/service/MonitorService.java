package com.lokins.sleepy.gui.service;

import com.lokins.sleepy.gui.network.ISleepyClient;
import com.lokins.sleepy.gui.utils.ConfigManager;
import com.lokins.sleepy.gui.utils.Win32WindowUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MonitorService {
    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);

    private final ISleepyClient client;
    private final Consumer<String> onAppChanged;
    private ScheduledExecutorService scheduler;
    private String lastApp = "";

    // 构造函数接收已经创建好的 client，不要在内部重新创建
    public MonitorService(ISleepyClient client, Consumer<String> onAppChanged) {
        this.client = client;
        this.onAppChanged = onAppChanged;
    }

    public boolean start() {
        if (client == null) {
            logger.error("监控服务启动失败：Client 未初始化。");
            return false;
        }

        // 直接使用传入的 client 连接，不要调用 Factory.createClient()
        if (!client.connect()) {
            logger.error("监控服务启动失败：无法建立连接。");
            return false;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Sleepy-Monitor-Thread");
            t.setDaemon(true);
            return t;
        });

        adaptiveSchedule();
        logger.info("监控服务已启动，使用协议: {}", client.getVersionTag());

        // 启动时立即触发一次 UI 更新，显示当前窗口
        checkCurrentWindow(true);
        return true;
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
        if (client != null) client.disconnect();
        logger.info("监控服务已停止。");
    }

    private void adaptiveSchedule() {
        if (scheduler == null || scheduler.isShutdown()) return;

        checkCurrentWindow(false);

        int delay = 5;
        try {
            delay = Integer.parseInt(ConfigManager.getInstance().get("settings", "interval", "5"));
        } catch (Exception ignored) {}

        scheduler.schedule(this::adaptiveSchedule, Math.max(delay, 1), TimeUnit.SECONDS);
    }

    private void checkCurrentWindow(boolean forceNotify) {
        try {
            String currentApp = Win32WindowUtil.getActiveWindowTitle();
            if (currentApp == null || currentApp.isEmpty()) currentApp = "未知窗口";

            // 执行上报
            client.sendReport(currentApp, true);

            // 如果窗口变了，或者强制通知（启动时），则触发回调更新 UI
            if (forceNotify || !currentApp.equals(lastApp)) {
                lastApp = currentApp;
                if (onAppChanged != null) {
                    onAppChanged.accept(currentApp);
                }
            }
        } catch (Exception e) {
            logger.error("扫描窗口异常: {}", e.getMessage());
        }
    }

    public ISleepyClient getClient() {
        return this.client;
    }
}