# LPlayer 开发指南

## 🚀 开发环境配置

### 必需工具
- **Android Studio**: Arctic Fox (2020.3.1) 或更高版本
- **JDK**: Java 11 或更高版本
- **Android SDK**: API Level 29+ (Android 10.0)
- **Gradle**: 7.4.2 或更高版本

### 推荐配置
```bash
# 内存配置 (gradle.properties)
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m -XX:+HeapDumpOnOutOfMemoryError

# 并行构建
org.gradle.parallel=true
org.gradle.caching=true

# 守护进程
org.gradle.daemon=true
```

## 📝 代码规范

### Java代码规范

#### 1. 命名规范
```java
// 类名：大驼峰命名法
public class VideoPlayerActivity extends AppCompatActivity

// 方法名：小驼峰命名法
public void playVideo(Uri videoUri)

// 常量：全大写，下划线分隔
private static final String TAG = "VideoPlayer";
private static final int REQUEST_CODE = 1001;

// 成员变量：小驼峰命名法，私有变量以m开头
private PlayerView mPlayerView;
private ExoPlayer mExoPlayer;
```

#### 2. 代码结构
```java
public class ExampleActivity extends AppCompatActivity {
    
    // 1. 常量定义
    private static final String TAG = "ExampleActivity";
    private static final int REQUEST_CODE = 1001;
    
    // 2. 成员变量
    private TextView mTitleTextView;
    private Button mActionButton;
    
    // 3. 生命周期方法
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);
        initViews();
        setupListeners();
    }
    
    // 4. 私有方法
    private void initViews() {
        mTitleTextView = findViewById(R.id.tv_title);
        mActionButton = findViewById(R.id.btn_action);
    }
    
    private void setupListeners() {
        mActionButton.setOnClickListener(v -> handleAction());
    }
    
    // 5. 事件处理方法
    private void handleAction() {
        // 处理逻辑
    }
}
```

#### 3. 注释规范
```java
/**
 * 视频播放器活动
 * 负责视频文件的播放控制和界面管理
 * 
 * @author Your Name
 * @version 1.0
 */
public class VideoPlayerActivity extends AppCompatActivity {
    
    /**
     * 播放指定的视频文件
     * 
     * @param videoUri 视频文件的URI
     * @param title 视频标题
     * @throws IllegalArgumentException 当URI无效时抛出
     */
    public void playVideo(Uri videoUri, String title) {
        if (videoUri == null) {
            throw new IllegalArgumentException("Video URI cannot be null");
        }
        
        // 播放逻辑实现
        mPlayer.setMediaItem(MediaItem.fromUri(videoUri));
        mPlayer.prepare();
        mPlayer.play();
    }
}
```

### XML布局规范

#### 1. 命名规范
```xml
<!-- 布局文件：activity_功能名.xml -->
activity_video_player.xml
activity_music_player.xml

<!-- 控件ID：类型_功能描述 -->
<TextView
    android:id="@+id/tv_video_title"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />

<Button
    android:id="@+id/btn_play_pause"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

#### 2. 布局结构
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".VideoPlayerActivity">

    <!-- 顶部工具栏 -->
    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- 播放器视图 -->
    <com.google.android.exoplayer2.ui.PlayerView
        android:id="@+id/player_view"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/control_panel"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <!-- 控制面板 -->
    <LinearLayout
        android:id="@+id/control_panel"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        app:layout_constraintBottom_toBottomOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

## 🔧 调试技巧

### 1. 日志输出
```java
public class DebugHelper {
    private static final String TAG = "LPlayer";
    
    // 调试日志
    public static void d(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }
    
    // 错误日志
    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
    
    // 性能日志
    public static void logTime(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        d(operation + " took " + duration + "ms");
    }
}
```

### 2. 异常处理
```java
public class ExceptionHandler {
    
    /**
     * 安全执行代码块，捕获异常并记录日志
     */
    public static void safeExecute(Runnable runnable, String operation) {
        try {
            runnable.run();
        } catch (Exception e) {
            Log.e("ExceptionHandler", "Error in " + operation, e);
            // 可以在这里添加崩溃报告或用户提示
        }
    }
    
    /**
     * 带返回值的异常处理
     */
    public static <T> T safeExecute(Supplier<T> supplier, T defaultValue, String operation) {
        try {
            return supplier.get();
        } catch (Exception e) {
            Log.e("ExceptionHandler", "Error in " + operation, e);
            return defaultValue;
        }
    }
}
```

### 3. 性能监控
```java
public class PerformanceMonitor {
    private static final Map<String, Long> startTimes = new HashMap<>();
    
    public static void startTimer(String operation) {
        startTimes.put(operation, System.currentTimeMillis());
    }
    
    public static void endTimer(String operation) {
        Long startTime = startTimes.remove(operation);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            Log.d("Performance", operation + " took " + duration + "ms");
        }
    }
    
    // 使用示例
    public static void example() {
        PerformanceMonitor.startTimer("file_scan");
        // 执行文件扫描
        scanFiles();
        PerformanceMonitor.endTimer("file_scan");
    }
}
```

## 🐛 常见问题解决

### 1. ExoPlayer相关问题

#### 问题：播放器无法播放视频
```java
// 解决方案：检查播放器状态和错误处理
player.addListener(new Player.Listener() {
    @Override
    public void onPlayerError(PlaybackException error) {
        Log.e(TAG, "播放器错误: " + error.getMessage());
        
        // 根据错误类型处理
        switch (error.errorCode) {
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
                showNetworkError();
                break;
            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS:
                showHttpError();
                break;
            default:
                showGenericError();
                break;
        }
    }
    
    @Override
    public void onPlaybackStateChanged(int state) {
        switch (state) {
            case Player.STATE_READY:
                // 播放器准备就绪
                break;
            case Player.STATE_BUFFERING:
                // 正在缓冲
                showBuffering();
                break;
            case Player.STATE_ENDED:
                // 播放结束
                handlePlaybackEnded();
                break;
        }
    }
});
```

#### 问题：全屏切换异常
```java
// 解决方案：正确处理屏幕方向变化
@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        // 进入全屏模式
        enterFullscreen();
    } else {
        // 退出全屏模式
        exitFullscreen();
    }
}

private void enterFullscreen() {
    // 隐藏状态栏和导航栏
    getWindow().setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    );
    
    // 隐藏系统UI
    View decorView = getWindow().getDecorView();
    decorView.setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_FULLSCREEN |
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    );
}
```

### 2. 权限相关问题

#### 问题：Android 11+ 存储权限
```java
// 解决方案：使用MediaStore API或SAF
public class StorageHelper {
    
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                   ContextCompat.checkSelfPermission(context, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    public static void requestStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
                activity.startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES_ACCESS);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES_ACCESS);
            }
        } else {
            ActivityCompat.requestPermissions(activity, 
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                REQUEST_STORAGE_PERMISSION);
        }
    }
}
```

### 3. 内存泄漏问题

#### 问题：Activity泄漏
```java
// 解决方案：正确管理生命周期
public class VideoPlayerActivity extends AppCompatActivity {
    
    private ExoPlayer mPlayer;
    private Handler mHandler;
    private Runnable mUpdateProgressRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化播放器
        mPlayer = new ExoPlayer.Builder(this).build();
        
        // 初始化Handler和Runnable
        mHandler = new Handler(Looper.getMainLooper());
        mUpdateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                updateProgress();
                mHandler.postDelayed(this, 1000);
            }
        };
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理资源
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        
        if (mHandler != null && mUpdateProgressRunnable != null) {
            mHandler.removeCallbacks(mUpdateProgressRunnable);
            mHandler = null;
        }
    }
}
```

## 📱 测试指南

### 1. 单元测试
```java
@RunWith(MockitoJUnitRunner.class)
public class PlaylistManagerTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private SharedPreferences mockPreferences;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    @Test
    public void testSaveVideoPlaylist() {
        // 准备测试数据
        List<VideoAdapter.VideoItem> videoList = createTestVideoList();
        
        // 模拟SharedPreferences行为
        when(mockPreferences.edit()).thenReturn(mockEditor);
        when(mockPreferences.getBoolean("save_video_playlist", false)).thenReturn(true);
        
        // 执行测试
        PlaylistManager.saveVideoPlaylist(mockContext, videoList);
        
        // 验证结果
        verify(mockEditor).putString(eq("saved_video_playlist"), anyString());
        verify(mockEditor).apply();
    }
    
    private List<VideoAdapter.VideoItem> createTestVideoList() {
        List<VideoAdapter.VideoItem> list = new ArrayList<>();
        list.add(new VideoAdapter.VideoItem(Uri.parse("content://test/video1"), "Test Video 1"));
        list.add(new VideoAdapter.VideoItem(Uri.parse("content://test/video2"), "Test Video 2"));
        return list;
    }
}
```

### 2. 仪器测试
```java
@RunWith(AndroidJUnit4.class)
public class VideoPlayerActivityTest {
    
    @Rule
    public ActivityTestRule<VideoPlayerActivity> activityRule = 
        new ActivityTestRule<>(VideoPlayerActivity.class);
    
    @Test
    public void testVideoPlayerControls() {
        // 测试播放按钮
        onView(withId(R.id.btn_play_pause))
            .perform(click());
        
        // 验证播放状态
        onView(withId(R.id.btn_play_pause))
            .check(matches(withDrawable(R.drawable.ic_pause)));
        
        // 测试进度条
        onView(withId(R.id.seek_bar))
            .perform(setProgress(50));
        
        // 验证进度更新
        onView(withId(R.id.tv_current_time))
            .check(matches(withText(containsString("00:30"))));
    }
}
```

### 3. 性能测试
```java
public class PerformanceTest {
    
    @Test
    public void testFileScanPerformance() {
        // 准备大量测试文件
        List<Uri> testFiles = createLargeFileList(1000);
        
        // 测量扫描时间
        long startTime = System.currentTimeMillis();
        List<VideoAdapter.VideoItem> result = scanFiles(testFiles);
        long endTime = System.currentTimeMillis();
        
        // 验证性能要求
        long duration = endTime - startTime;
        assertTrue("文件扫描应在1秒内完成", duration < 1000);
        assertEquals("应扫描所有文件", 1000, result.size());
    }
}
```

## 🚀 性能优化

### 1. RecyclerView优化
```java
public class OptimizedVideoAdapter extends RecyclerView.Adapter<VideoViewHolder> {
    
    // 使用DiffUtil优化列表更新
    private final DiffUtil.ItemCallback<VideoItem> diffCallback = 
        new DiffUtil.ItemCallback<VideoItem>() {
            @Override
            public boolean areItemsTheSame(VideoItem oldItem, VideoItem newItem) {
                return oldItem.getUri().equals(newItem.getUri());
            }
            
            @Override
            public boolean areContentsTheSame(VideoItem oldItem, VideoItem newItem) {
                return oldItem.getDisplayName().equals(newItem.getDisplayName()) &&
                       oldItem.getDuration() == newItem.getDuration();
            }
        };
    
    private final AsyncListDiffer<VideoItem> differ = new AsyncListDiffer<>(this, diffCallback);
    
    public void submitList(List<VideoItem> newList) {
        differ.submitList(newList);
    }
}
```

### 2. 图片加载优化
```java
public class ImageLoader {
    
    private static final LruCache<String, Bitmap> memoryCache = 
        new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024) / 8);
    
    public static void loadImage(String url, ImageView imageView) {
        // 先从内存缓存加载
        Bitmap cachedBitmap = memoryCache.get(url);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }
        
        // 异步加载图片
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... urls) {
                return downloadBitmap(urls[0]);
            }
            
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    // 缓存到内存
                    memoryCache.put(url, bitmap);
                    imageView.setImageBitmap(bitmap);
                }
            }
        }.execute(url);
    }
}
```

## 📚 学习资源

### 1. 官方文档
- [Android Developer](https://developer.android.com/)
- [ExoPlayer Documentation](https://exoplayer.dev/)
- [Material Design Guidelines](https://material.io/design)

### 2. 推荐书籍
- 《Android编程权威指南》
- 《Android开发艺术探索》
- 《Effective Java》

### 3. 在线课程
- [Udacity Android Development](https://www.udacity.com/course/android-basics-nanodegree-by-google--nd803)
- [Coursera Android Development](https://www.coursera.org/specializations/android-app-development)

---

遵循这些开发指南，您将能够编写出高质量、可维护的代码，并为LPlayer项目做出有价值的贡献。
