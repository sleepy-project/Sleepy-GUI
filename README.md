# Sleepy-GUI

<p align="center">
  <img src="src/main/resources/images/icon.png" alt="Sleepy-GUI Logo" width="128"/>
</p>

Sleepy-GUI 是 [sleepy-project/sleepy](https://github.com/sleepy-project/sleepy) v5.x 版本的图形化配置客户端。它提供了一个直观的界面，帮助用户更方便地配置和管理 sleepy 服务端，无需手动编辑配置文件或记忆复杂命令。

## ✨ 特性

- 🖥️ **简洁的图形界面**：基于 Java Swing/JavaFX（根据实际情况调整）构建，跨平台运行。
- ⚙️ **一站式配置**：支持对 sleepy 服务端的各项参数进行可视化调整。
- 🔗 **无缝对接**：与 sleepy v5.x 服务端直接通信，实时生效。
- 📦 **开箱即用**：提供预编译的可执行 JAR 包，下载后即可运行。
- 🧩 **开源免费**：基于 MIT 许可证，欢迎贡献和二次开发。

## 🚀 快速开始

### 环境准备

1. **Java 运行时环境 (JRE)** 或 **Java 开发工具包 (JDK)** 8 或更高版本。  
   可通过命令 `java -version` 检查。

2. **Sleepy 服务端**：确保已在本地或远程服务器上安装并运行了 sleepy 项目的 v5.x 版本。  
   （参考 [sleepy 官方文档](https://github.com/sleepy-project/sleepy) 进行安装，若链接失效请自行搜索）

### 下载与运行

#### 方法一：使用预编译 JAR 包（推荐）

1. 前往 [Releases 页面](https://github.com/NoClassFoundError/Sleepy-GUI/releases) 下载最新版本的 `sleepy-gui-x.x.x.jar`。
2. 打开终端（或命令提示符），进入 JAR 包所在目录，执行：（请将 x.x.x 替换为实际版本号）
    ```bash
    java -jar sleepy-gui-x.x.x.jar
    ```
#### 方法二：从源码构建运行
1. 克隆本仓库：
   ```bash
    git clone https://github.com/NoClassFoundError/Sleepy-GUI.git
    cd Sleepy-GUI
    ```
2. 使用 Gradle Wrapper 构建可执行 JAR（包含所有依赖）
    ```bash
    ./gradlew fatJar        # Linux/macOS
     ```
3. 构建完成后，在 build/libs/ 目录下会生成 sleepy-gui-x.x.x.jar，按方法一运行即可

## 📄 许可证
本项目基于 MIT 许可证 开源。详细信息请参见项目根目录下的 [LICENSE](https://github.com/NoClassFoundError/Sleepy-GUI/blob/master/LICENSE) 文件。

## 🙏 致谢
感谢 [sleepy-project/sleepy](https://github.com/sleepy-project/sleepy) 提供的API和技术支持。