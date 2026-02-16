package com.lokins.sleepy.gui.controller;

import com.lokins.sleepy.gui.network.ISleepyClient;
import com.lokins.sleepy.gui.network.SleepyClientFactory;
import com.lokins.sleepy.gui.network.SleepyClientV5;
import com.lokins.sleepy.gui.network.SleepyPingProvider;
import com.lokins.sleepy.gui.service.MonitorService;
import com.lokins.sleepy.gui.utils.ConfigManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class ConnectViewController {
    private static final Logger logger = LoggerFactory.getLogger(ConnectViewController.class);

    @FXML private TextField serverUrlField, deviceNameField;
    @FXML private PasswordField secretField, refreshTokenField;
    @FXML private Label statusLabel, refreshLabel;
    @FXML private Button startBtn, testConnBtn;
    @FXML private CheckBox autoProtocolCheck;
    @FXML private ChoiceBox<String> protocolVersionBox;

    private static MonitorService monitorService;
    private static boolean isRunning = false;

    @FXML
    public void initialize() {
        // 1. 初始化下拉框选项（仅执行一次）
        if (protocolVersionBox.getItems().isEmpty()) {
            protocolVersionBox.getItems().addAll("Protocol v5 (Legacy)", "Protocol v6 (Modern)");
        }

        loadConfiguration();

        if (isRunning && monitorService != null) {
            String currentVersion = monitorService.getClient().getVersionTag(); // 需在 MonitorService 增加 getClient
            if (currentVersion.contains("v6")) {
                protocolVersionBox.setValue("Protocol v6 (Modern)");
            } else {
                protocolVersionBox.setValue("Protocol v5 (Legacy)");
            }
            updateUIState(); // 锁定所有输入框
        }

        // 4. 【核心修复】监听下拉框，改变即保存
        protocolVersionBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isRunning) {
                updateRefreshFieldState(); // 更新刷新密钥置灰状态
                saveCurrentConfig();      // 只要改了就保存，防止切换 Tab 丢失
            }
        });

        if (isRunning) {
            statusLabel.setText("状态: 正在监控 (" + protocolVersionBox.getValue() + ")");
        } else {
            statusLabel.setText("状态: 已停止");
        }
    }

    private void updateRefreshFieldState() {
        boolean isV6 = protocolVersionBox.getValue().contains("v6");
        boolean isAuto = autoProtocolCheck.isSelected();
        // 只有非自动模式且选了 V6，或者自动模式（待检测）时，我们根据需要开启
        refreshTokenField.setDisable(!isV6 && !isAuto);
        refreshLabel.setOpacity(refreshTokenField.isDisable() ? 0.5 : 1.0);
    }

    @FXML
    private void handleAutoProtocolToggle() {
        protocolVersionBox.setDisable(autoProtocolCheck.isSelected());
        updateRefreshFieldState();
    }

    @FXML
    private void handleTestConnection() {
        String url = serverUrlField.getText().trim();
        if (url.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提醒", "请输入服务器地址后再进行测试");
            return;
        }

        // UI 反馈
        testConnBtn.setDisable(true);
        statusLabel.setText("状态: 正在测试连通性...");

        // 在后台线程执行网络操作，避免 UI 卡死
        new Thread(() -> {
            SleepyPingProvider.PingResult result = SleepyPingProvider.testConnection(url);

            // 回到 UI 线程更新界面
            Platform.runLater(() -> {
                testConnBtn.setDisable(false);
                if (result.success) {
                    statusLabel.setText("状态: 连接正常 (" + result.latencyMs + "ms)");
                    showAlert(Alert.AlertType.INFORMATION, "测试通过",
                            "成功连接到服务器！\n响应延迟: " + result.latencyMs + "ms");
                } else {
                    statusLabel.setText("状态: 连接失败");
                    showAlert(Alert.AlertType.ERROR, "测试失败", result.message);
                }
            });
        }).start();
    }

    @FXML
    private void handleStart() {
        if (isRunning) {
            stopMonitoring();
            return;
        }

        saveCurrentConfig();

        if (autoProtocolCheck.isSelected()) {
            statusLabel.setText("状态: 自动识别中...");
            new Thread(() -> {
                int version = SleepyClientV5.probeProtocolVersion(serverUrlField.getText());
                Platform.runLater(() -> executeStartSequence(version));
            }).start();
        } else {
            executeStartSequence(protocolVersionBox.getValue().contains("v6") ? 6 : 5);
        }
    }

    private void executeStartSequence(int version) {
        // 确保在主线程更新 UI 状态
        Platform.runLater(() -> {
            // 如果是自动检测出来的版本，我们需要手动同步下拉框的值
            // 这样 updateRefreshFieldState() 才能拿到正确的结果
            if (autoProtocolCheck.isSelected()) {
                if (version == 6) {
                    protocolVersionBox.setValue("Protocol v6 (Modern)");
                } else {
                    protocolVersionBox.setValue("Protocol v5 (Legacy)");
                }
            }
            // 核心修复：强制刷新“刷新密钥”一栏的禁用/启用状态
            updateRefreshFieldState();
        });

        // 原有的启动逻辑...
        ISleepyClient client = SleepyClientFactory.createClient();
        monitorService = new MonitorService(client, (appName) -> {
            Platform.runLater(() -> statusLabel.setText("正在运行: " + appName));
        });

        if (monitorService.start()) {
            isRunning = true;
            Platform.runLater(this::updateUIState); // 务必在 UI 线程刷新按钮文字
        } else {
            Platform.runLater(() -> {
                statusLabel.setText("状态: 连接失败");
                isRunning = false;
                updateUIState();
            });
        }
    }

    private void stopMonitoring() {
        if (monitorService != null) monitorService.stop();
        isRunning = false;
        updateUIState();
    }

    @FXML
    private void handleSaveAction() {
        saveCurrentConfig();
        showAlert(Alert.AlertType.INFORMATION, "成功", "配置已保存");
    }

    private void saveCurrentConfig() {
        try {
            ConfigManager config = ConfigManager.getInstance();
            config.set("server", "url", serverUrlField.getText());
            config.set("auth", "secret", secretField.getText());
            config.set("auth", "refresh_token", refreshTokenField.getText()); // 存入 auth 下
            config.set("device", "name", deviceNameField.getText());
            config.set("settings", "auto_protocol", String.valueOf(autoProtocolCheck.isSelected()));
            config.set("settings", "protocol_version", autoProtocolCheck.isSelected() ? "0" : (protocolVersionBox.getValue().contains("v6") ? "6" : "5"));
            config.save();
        } catch (IOException e) { logger.error("保存失败", e); }
    }

    private void loadConfiguration() {
        try {
            ConfigManager config = ConfigManager.getInstance();
            serverUrlField.setText(config.get("server", "url", ""));
            secretField.setText(config.get("auth", "secret", ""));
            refreshTokenField.setText(config.get("auth", "refresh_token", ""));
            deviceNameField.setText(config.get("device", "name", "My-PC"));
            boolean isAuto = Boolean.parseBoolean(config.get("settings", "auto_protocol", "true"));
            autoProtocolCheck.setSelected(isAuto);
            protocolVersionBox.setDisable(isAuto);
            updateRefreshFieldState();
        } catch (Exception e) { logger.warn("配置加载失败"); }
    }

    private void updateUIState() {
        // 强制 UI 切换
        startBtn.setText(isRunning ? "停止监控" : "开始监控");
        startBtn.getStyleClass().removeAll("primary-button", "danger-button");
        startBtn.getStyleClass().add(isRunning ? "danger-button" : "primary-button");

        // 运行中禁用，停止后启用
        boolean disabled = isRunning;
        serverUrlField.setDisable(disabled);
        secretField.setDisable(disabled);
        refreshTokenField.setDisable(disabled || !protocolVersionBox.getValue().contains("v6")); // V6 特殊判断
        deviceNameField.setDisable(disabled);
        autoProtocolCheck.setDisable(disabled);
        protocolVersionBox.setDisable(disabled || autoProtocolCheck.isSelected());
        testConnBtn.setDisable(disabled);
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}