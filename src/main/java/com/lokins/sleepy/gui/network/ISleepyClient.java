package com.lokins.sleepy.gui.network;

import java.util.Map;

/**
 * 客户端统一接口
 * 抽象了 Sleepy 协议不同版本（v5/v6）的核心行为。
 */
public interface ISleepyClient {

    /**
     * 执行连接与身份验证
     * v5: 验证 URL 和 Secret 有效性。
     * v6: 获取 Token/Refresh Token，并根据配置决定是否开启 WebSocket。
     * @return 如果连接成功并可以开始上报，返回 true
     */
    boolean connect();

    /**
     * 优雅地断开连接
     * 停止调度任务、关闭长连接并清理内存中的 Token。
     */
    void disconnect();

    /**
     * 核心上报方法
     * @param status 状态描述（通常为活跃窗口标题）
     * @param isUsing 用户当前是否处于活跃状态（非空闲）
     */
    void sendReport(String status, boolean isUsing);

    /**
     * 增强版上报方法 (支持扩展字段)
     * 对应 v6 协议中的 fields: HashMap<String, String>
     * @param status 状态描述
     * @param isUsing 是否活跃
     * @param additionalFields 附加信息（如系统负载、自定义标记等）
     */
    void sendReport(String status, boolean isUsing, Map<String, Object> additionalFields);

    // 修复 2: 实现接口要求的 ping 方法
    boolean ping();

    /**
     * 检查客户端健康状况
     * 包括网络连通性、Token 是否过期且无法刷新、WebSocket 是否意外断开等。
     */
    boolean isAlive();

    /**
     * 获取协议描述符
     * @return 返回 "v5"、"v6 (WebSocket)" 等，用于 UI 状态栏展示
     */
    String getVersionTag();

    /**
     * 获取当前延迟 (ms)
     * 可选实现，用于 UI 实时展示心跳质量
     */
    default long getLatency() {
        return -1;
    }
}