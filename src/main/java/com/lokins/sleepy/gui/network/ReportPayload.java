package com.lokins.sleepy.gui.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class ReportPayload {

    @JsonProperty("id")
    private String id; // 设备唯一标识符，例如 "pc-01"

    @JsonProperty("show_name")
    private String showName; // 显示名称

    @JsonProperty("using")
    private boolean using = true; // 是否正在使用，默认为 true

    @JsonProperty("status")
    private String status; // 对应你的应用名 (App Name)

    @JsonProperty("fields")
    private Map<String, Object> fields = new HashMap<>(); // 其他扩展参数

    public ReportPayload(String id, String showName, String appName) {
        this.id = id;
        this.showName = showName;
        this.status = appName;
    }

    public void setUsing(boolean using) {
        this.using = using;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getFields() {
        return fields;
    }
}