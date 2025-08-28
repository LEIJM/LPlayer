# LPlayer 项目架构说明

## 🏗️ 整体架构

LPlayer采用传统的Android架构模式，基于Activity + Fragment的组合，结合ExoPlayer媒体播放引擎，构建了一个功能完整的媒体播放器应用。

```
┌─────────────────────────────────────────────────────────────┐
│                        LPlayer App                         │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ MainActivity│  │PlayerActivity│  │MusicPlayer  │        │
│  │             │  │             │  │  Activity   │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────┤
│                    Fragment Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │VideoFragment│  │MusicFragment│  │PlaylistFrag │        │
│  │             │  │             │  │             │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│  ┌─────────────┐  ┌─────────────┐                         │
│  │SettingsFrag │  │ AboutFrag   │                         │
│  │             │  │             │                         │
│  └─────────────┘  └─────────────┘                         │
├─────────────────────────────────────────────────────────────┤
│                   Adapter Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │VideoAdapter │  │MusicAdapter │  │ViewPager    │        │
│  │             │  │             │  │  Adapter    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
├─────────────────────────────────────────────────────────────┤
│                   Manager Layer                            │
│  ┌─────────────┐  ┌─────────────┐                         │
│  │PlaylistMgr  │  │PlaybackMgr  │                         │
│  │             │  │             │                         │
│  └─────────────┘  └─────────────┘                         │
├─────────────────────────────────────────────────────────────┤
│                   ExoPlayer Engine                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Core      │  │     UI      │  │    DASH     │        │
│  │             │  │             │  │             │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
│  ┌─────────────┐  ┌─────────────┐                         │
│  │    HLS      │  │    RTSP     │                         │
│  │             │  │             │                         │
│  └─────────────┘  └─────────────┘                         │
├─────────────────────────────────────────────────────────────┤
│                   Data Layer                               │
│  ┌─────────────┐  ┌─────────────┐                         │
│  │SharedPrefs  │  │   Gson      │                         │
│  │             │  │             │                         │
│  └─────────────┘  └─────────────┘                         │
└─────────────────────────────────────────────────────────────┘
```

## 📱 核心组件详解

### 1. MainActivity - 主控制器

**职责**: 
- 管理底部导航栏和Fragment切换
- 处理文件选择和权限请求
- 协调视频和音乐播放列表
- 管理应用生命周期

**关键特性**:
```java
public class MainActivity extends AppCompatActivity 
    implements VideoFragment.VideoFragmentListener, 
               PlaylistFragment.PlaylistFragmentListener,
               MusicFragment.MusicFragmentListener {
    
    // 底部导航管理
    private BottomNavigationView bottomNavigationView;
    private ViewPager2 viewPager;
    
    // 播放列表管理
    private List<VideoAdapter.VideoItem> videoList;
    private int currentPlayingPosition = -1;
    
    // 权限和文件选择
    private ActivityResultLauncher<Intent> videoPickerLauncher;
    private ActivityResultLauncher<Intent> folderPickerLauncher;
}
```

**设计模式**: 
- **观察者模式**: 通过接口回调与Fragment通信
- **策略模式**: 不同的文件选择策略（单个文件 vs 文件夹）

### 2. PlayerActivity - 视频播放器

**职责**:
- 视频播放控制
- 全屏模式管理
- 播放列表显示
- 播放速度控制

**核心功能**:
```java
public class PlayerActivity extends AppCompatActivity {
    private PlayerView playerView;
    private ExoPlayer player;
    
    // 播放控制
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton fullscreenButton;
    
    // 播放列表管理
    private RecyclerView playlistRecyclerView;
    private VideoAdapter playlistAdapter;
    
    // 双击检测
    private long lastClickTimePrevious = 0;
    private long lastClickTimeNext = 0;
}
```

**设计模式**:
- **状态模式**: 播放状态管理（播放、暂停、缓冲等）
- **命令模式**: 播放控制命令（播放、暂停、快进等）

### 3. MusicPlayerActivity - 音乐播放器

**职责**:
- 音频播放控制
- 播放模式管理（循环、随机）
- 音乐播放列表
- 音频元数据显示

**特色功能**:
```java
public class MusicPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    
    // 播放模式
    private boolean isRepeatMode = false;
    private boolean isShuffleMode = false;
    
    // 播放列表
    private List<MusicAdapter.MusicItem> playlistItems;
    private int currentPlayingPosition = -1;
    
    // 自动播放控制
    private boolean autoPlayNext = true;
}
```

**设计模式**:
- **策略模式**: 不同的播放模式策略
- **观察者模式**: 播放状态变化监听

### 4. Fragment组件架构

#### VideoFragment
- 视频文件列表显示
- 文件选择界面
- 与MainActivity通信

#### MusicFragment  
- 音乐文件列表显示
- 音乐文件夹选择
- 音乐播放控制

#### PlaylistFragment
- 播放列表管理
- 播放历史记录
- 播放列表编辑

#### SettingsFragment
- 应用设置界面
- 文件夹权限管理
- 播放偏好设置

#### AboutFragment
- 应用信息显示
- 功能特性介绍
- 版本信息

### 5. 适配器层设计

#### VideoAdapter
```java
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    public static class VideoItem {
        private Uri uri;
        private String displayName;
        private long duration;
        private long size;
    }
}
```

#### MusicAdapter
```java
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {
    public static class MusicItem {
        private Uri uri;
        private String title;
        private String artist;
        private String album;
        private long duration;
    }
}
```

#### ViewPagerAdapter
- 管理Fragment页面切换
- 支持底部导航同步

### 6. 管理器层设计

#### PlaylistManager
**职责**: 播放列表的持久化存储和加载

**实现方式**:
```java
public class PlaylistManager {
    // 使用SharedPreferences + Gson进行数据持久化
    private static final String KEY_VIDEO_PLAYLIST = "saved_video_playlist";
    private static final String KEY_MUSIC_PLAYLIST = "saved_music_playlist";
    
    // 序列化支持
    public static class SerializableVideoItem {
        public String uri;
        public String displayName;
    }
}
```

**设计模式**:
- **单例模式**: 全局播放列表管理
- **工厂模式**: 创建不同类型的播放列表项

#### PlaybackManager
**职责**: 播放控制逻辑管理

**功能**:
- 播放状态管理
- 播放队列控制
- 播放模式切换

## 🔄 数据流设计

### 1. 文件选择流程

```
用户选择文件 → MainActivity接收 → 权限检查 → 文件扫描 → 
更新播放列表 → 通知Fragment → 界面刷新
```

### 2. 播放控制流程

```
用户操作 → PlayerActivity处理 → ExoPlayer执行 → 
状态回调 → 界面更新 → 播放列表同步
```

### 3. 播放列表持久化流程

```
播放列表变化 → PlaylistManager处理 → Gson序列化 → 
SharedPreferences存储 → 应用重启后恢复
```

## 🎯 设计原则

### 1. 单一职责原则
- 每个Activity/Fragment只负责特定的功能模块
- 适配器只处理数据绑定和视图更新
- 管理器类专注于特定的业务逻辑

### 2. 开闭原则
- 通过接口定义Fragment通信协议
- 支持扩展新的媒体格式和播放模式
- 设置系统支持动态配置项

### 3. 依赖倒置原则
- 高层模块不依赖低层模块
- 通过接口进行模块间通信
- ExoPlayer作为抽象层，支持不同的媒体源

### 4. 接口隔离原则
- 为不同的功能定义专门的接口
- Fragment监听器接口职责单一
- 避免臃肿的通用接口

## 🚀 扩展性设计

### 1. 媒体格式扩展
```java
// 支持新的媒体格式
public interface MediaFormatHandler {
    boolean canHandle(Uri uri);
    MediaItem createMediaItem(Uri uri);
}
```

### 2. 播放控制扩展
```java
// 自定义播放控制
public interface CustomPlaybackControl {
    void onCustomAction(String action, Bundle extras);
}
```

### 3. 界面主题扩展
```java
// 主题切换支持
public interface ThemeManager {
    void applyTheme(int themeId);
    void toggleDarkMode();
}
```

## 📊 性能优化

### 1. 内存管理
- 使用ViewHolder模式减少对象创建
- 及时释放ExoPlayer资源
- 图片加载使用缓存机制

### 2. 异步处理
- 文件扫描在后台线程执行
- 播放列表加载异步化
- UI更新在主线程进行

### 3. 缓存策略
- 播放列表缓存到SharedPreferences
- 文件夹权限持久化
- 播放进度状态保存

## 🔒 安全性考虑

### 1. 权限管理
- 运行时权限请求
- URI权限持久化
- 最小权限原则

### 2. 数据验证
- 文件类型检查
- URI有效性验证
- 输入参数校验

### 3. 异常处理
- 播放错误处理
- 文件访问异常处理
- 网络异常处理

---

这个架构设计确保了LPlayer具有良好的可维护性、可扩展性和性能表现，为后续功能增强和代码重构提供了坚实的基础。
