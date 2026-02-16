plugins {
    application
    java
    id("org.openjfx.javafxplugin") version "0.0.14"
}

group = "com.lokins.sleepy.gui"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {

    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // JSON 处理
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    // 配置文件
    implementation("org.ini4j:ini4j:0.5.4")
    // Windows 原生 API
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    // 日志
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.fxml", "javafx.graphics")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.lokins.sleepy.gui.Launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.register<Jar>("fatJar") {
    group = "build"
    description = "创建轻量级引导 JAR (不含 JavaFX)"

    outputs.upToDateWhen { false }
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    archiveBaseName.set("sleepy-gui")
    archiveVersion.set(project.version.toString())

    manifest {
        attributes["Main-Class"] = "com.lokins.sleepy.gui.launch.Launcher"
    }

    from(sourceSets.main.get().output)

    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .filter { !it.name.contains("javafx") }
            .map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}