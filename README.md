# LPlayer - 多功能媒体播放器

[![Android](https://img.shields.io/badge/Android-29+-green.svg)](https://developer.android.com/about/versions/android-13)
[![API Level](https://img.shields.io/badge/API%20Level-29--34-blue.svg)](https://developer.android.com/about/versions)
[![ExoPlayer](https://img.shields.io/badge/ExoPlayer-2.18.7-orange.svg)](https://exoplayer.dev/)

LPlayer 是一个功能强大的Android媒体播放器应用，支持视频和音频播放，具有现代化的用户界面和丰富的功能特性。

## 🎯 主要功能

### 视频播放
- **多格式支持**: 支持MP4、AVI、MKV、MOV等主流视频格式
- **ExoPlayer引擎**: 基于Google ExoPlayer 2.18.7，提供流畅的播放体验
- **播放控制**: 播放/暂停、快进/快退、音量调节
- **全屏模式**: 支持横竖屏切换和全屏播放
- **播放速度**: 支持0.5x - 2.0x倍速播放
- **播放列表**: 支持文件夹扫描和播放列表管理

### 音频播放
- **音乐播放器**: 独立的音乐播放界面
- **播放模式**: 单曲循环、列表循环、随机播放
- **音频控制**: 播放控制、进度条、音量调节
- **专辑信息**: 显示音乐标题、艺术家信息

### 文件管理
- **文件夹扫描**: 支持选择本地文件夹进行媒体扫描
- **权限管理**: 智能处理Android存储权限
- **播放列表保存**: 可选择性保存播放列表到本地
- **默认文件夹**: 支持设置默认视频和音乐文件夹

### 用户界面
- **Material Design**: 现代化的Material Design界面
- **底部导航**: 视频、音乐、播放列表、设置四个主要模块
- **深色主题**: 支持深色主题模式
- **响应式布局**: 适配不同屏幕尺寸和方向

## 🏗️ 项目架构

### 核心组件

```
app/src/main/java/com/example/lplayer/
├── MainActivity.java              # 主活动，管理底部导航和Fragment
├── PlayerActivity.java            # 视频播放器活动
├── MusicPlayerActivity.java       # 音乐播放器活动
├── PlaylistManager.java           # 播放列表管理器
├── PlaybackManager.java           # 播放控制管理器
├── fragments/                     # Fragment组件
│   ├── VideoFragment.java         # 视频列表Fragment
│   ├── MusicFragment.java         # 音乐列表Fragment
│   ├── PlaylistFragment.java      # 播放列表Fragment
│   ├── SettingsFragment.java      # 设置Fragment
│   └── AboutFragment.java         # 关于Fragment
├── adapters/                      # 适配器组件
│   ├── VideoAdapter.java          # 视频列表适配器
│   ├── MusicAdapter.java          # 音乐列表适配器
│   └── ViewPagerAdapter.java      # 页面适配器
└── utils/                         # 工具类
```

### 技术架构

- **架构模式**: 基于Activity + Fragment的传统Android架构
- **媒体播放**: 使用ExoPlayer作为核心播放引擎
- **数据存储**: SharedPreferences + Gson进行播放列表持久化
- **权限管理**: 运行时权限请求和URI权限管理
- **UI框架**: AndroidX + Material Design组件

## 🚀 快速开始

### 环境要求

- Android Studio Arctic Fox (2020.3.1) 或更高版本
- Android SDK 29 (API Level 29) 或更高版本
- Java 11 或更高版本
- Gradle 7.4.2 或更高版本

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/LEIJM/LPlayer.git
   cd LPlayer
   ```

2. **打开项目**
   - 在Android Studio中打开项目
   - 等待Gradle同步完成

3. **配置SDK**
   - 确保已安装Android SDK 29+
   - 在`local.properties`中配置SDK路径

4. **构建运行**
   - 连接Android设备或启动模拟器
   - 点击运行按钮或使用`./gradlew installDebug`

### 权限配置

应用需要以下权限：

```xml
<!-- 存储权限 -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 📱 使用指南

### 视频播放

1. **选择视频文件**
   - 点击"选择视频"按钮选择单个视频文件
   - 或点击"选择文件夹"扫描整个文件夹

2. **播放控制**
   - 播放/暂停: 点击播放按钮
   - 快进/快退: 双击上一曲/下一曲按钮
   - 全屏: 点击全屏按钮
   - 进度控制: 拖动进度条

3. **播放列表管理**
   - 查看播放列表: 点击播放列表按钮
   - 切换视频: 点击列表中的视频项
   - 保存播放列表: 在设置中启用播放列表保存

### 音乐播放

1. **选择音乐文件夹**
   - 在音乐标签页选择音乐文件夹
   - 应用会自动扫描音频文件

2. **播放控制**
   - 播放模式: 循环、随机播放
   - 播放控制: 上一曲、下一曲、播放/暂停
   - 进度控制: 拖动进度条调整播放位置

### 设置配置

1. **播放设置**
   - 默认播放速度
   - 自动播放下一个
   - 播放列表保存选项

2. **文件夹设置**
   - 设置默认视频文件夹
   - 设置默认音乐文件夹
   - 管理文件夹权限

## 🔧 开发指南

### 项目结构

```
LPlayer/
├── app/                          # 应用模块
│   ├── build.gradle.kts         # 应用级构建配置
│   ├── src/main/                # 主要源代码
│   │   ├── java/                # Java源代码
│   │   ├── res/                 # 资源文件
│   │   └── AndroidManifest.xml  # 应用清单
│   └── proguard-rules.pro       # 代码混淆规则
├── build.gradle.kts             # 项目级构建配置
├── gradle/                      # Gradle配置
├── gradlew                      # Gradle包装器脚本
└── README.md                    # 项目说明文档
```

### 关键依赖

```kotlin
// ExoPlayer媒体播放库
implementation("com.google.android.exoplayer:exoplayer-core:2.18.7")
implementation("com.google.android.exoplayer:exoplayer-dash:2.18.7")
implementation("com.google.android.exoplayer:exoplayer-ui:2.18.7")
implementation("com.google.android.exoplayer:exoplayer-hls:2.18.7")
implementation("com.google.android.exoplayer:exoplayer-rtsp:2.18.7")

// JSON序列化
implementation("com.google.code.gson:gson:2.9.0")

// UI组件
implementation("com.google.android.material:material:1.4.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.1")
implementation("androidx.recyclerview:recyclerview:1.2.1")
```

### 核心类说明

#### MainActivity
- 主活动，管理底部导航和Fragment切换
- 处理文件选择和权限请求
- 管理视频和音乐播放列表

#### PlayerActivity
- 视频播放器核心实现
- 基于ExoPlayer的播放控制
- 全屏模式和播放控制界面

#### MusicPlayerActivity
- 音乐播放器实现
- 播放模式控制（循环、随机）
- 音乐播放列表管理

#### PlaylistManager
- 播放列表的持久化存储
- 使用SharedPreferences + Gson保存播放列表
- 支持视频和音乐播放列表管理

### 扩展开发

#### 添加新的媒体格式支持
```java
// 在PlayerActivity中添加新的媒体类型支持
MediaItem mediaItem = MediaItem.fromUri(videoUri);
player.setMediaItem(mediaItem);
```

#### 自定义播放控制
```java
// 实现自定义播放控制逻辑
player.addListener(new Player.Listener() {
    @Override
    public void onPlaybackStateChanged(int state) {
        // 自定义播放状态处理
    }
});
```

#### 添加新的设置选项
```xml
<!-- 在res/xml/preferences.xml中添加新的设置项 -->
<Preference
    android:key="custom_setting"
    android:title="自定义设置"
    android:summary="设置描述" />
```

## 🧪 测试

### 单元测试
```bash
./gradlew test
```

### 仪器测试
```bash
./gradlew connectedAndroidTest
```

### 构建测试
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## 📦 构建和发布

### Debug版本
```bash
./gradlew assembleDebug
```

### Release版本
```bash
./gradlew assembleRelease
```

### 生成APK
构建完成后，APK文件位于：
```
app/build/outputs/apk/release/app-release.apk
```

## 🤝 贡献指南

1. Fork项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开Pull Request

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## 🙏 致谢

- [ExoPlayer](https://exoplayer.dev/) - Google提供的强大媒体播放库
- [Material Design](https://material.io/) - Google的现代化设计语言
- [AndroidX](https://developer.android.com/jetpack/androidx) - Android支持库

## 📞 联系方式

- 项目主页: [GitHub Repository](https://github.com/LEIJM/LPlayer)
- 问题反馈: [Issues](https://github.com/LEIJM/LPlayer/issues)
- 功能建议: [Discussions](https://github.com/LEIJM/LPlayer/discussions)

---

**LPlayer** - 让媒体播放更简单、更强大！ 🎵🎬
