package com.example.lplayer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import android.database.Cursor;
import android.provider.MediaStore;
import androidx.preference.PreferenceManager;

public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // 双击检测时间间隔（毫秒）
    
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView titleTextView;
    private TextView currentTimeTextView;
    private TextView durationTextView;
    private SeekBar seekBar;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton fullscreenButton;
    private ImageButton backButton;
    private ImageButton lockButton;
    private ImageButton moreButton;
    private ImageButton playlistButton;
    
    private ConstraintLayout topControls;
    private ConstraintLayout bottomControls;
    private LinearLayout controlButtons;
    private ConstraintLayout playlistPanel;
    private androidx.cardview.widget.CardView playlistPanelCard;
    private RecyclerView playlistRecyclerView;
    private TextView playlistEmptyView;
    
    private String videoTitle;
    private Uri videoUri;
    private boolean isFullscreen = false;
    private boolean isPlaying = false;
    private boolean isLocked = false;
    private boolean controlsVisible = true;
    private boolean playlistVisible = false;
    
    private VideoAdapter playlistAdapter;
    
    // 双击检测变量
    private long lastClickTimePrevious = 0;
    private long lastClickTimeNext = 0;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControls;
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 1000);
        }
    };
    
    private StringBuilder formatBuilder;
    private Formatter formatter;

    // 恢复本地播放列表和播放位置
    private List<VideoAdapter.VideoItem> playlistItems = new ArrayList<>();
    private int currentPlayingPosition = -1;

    // 标志位用于区分用户操作和自动播放结束
    private boolean isUserNext = false;
    private boolean isUserPrevious = false;
    private boolean isUserSelect = false;
    private boolean autoPlayNext = true; // 添加自动播放设置变量

    private ImageButton unlockButton; // 动态解锁按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        
        // 读取自动播放设置
        autoPlayNext = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("auto_play_next", true);
        
        // 全屏设置
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        
        try {
            // 从Intent获取数据
            if (getIntent() != null) {
                String uriString = getIntent().getStringExtra("video_uri");
                videoTitle = getIntent().getStringExtra("video_title");
                if (uriString != null) {
                    videoUri = Uri.parse(uriString);
                    Log.d(TAG, "视频URI: " + uriString);
                } else {
                    Log.e(TAG, "未提供视频URI");
                }
                // 获取播放列表数据（如果有）
                if (getIntent().hasExtra("playlist")) {
                    ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("playlist");
                    ArrayList<String> titles = getIntent().getStringArrayListExtra("playlist_titles");
                    currentPlayingPosition = getIntent().getIntExtra("current_position", 0);
                    if (uriStrings != null && titles != null && uriStrings.size() == titles.size()) {
                        for (int i = 0; i < uriStrings.size(); i++) {
                            Uri uri = Uri.parse(uriStrings.get(i));
                            String title = titles.get(i);
                            playlistItems.add(new VideoAdapter.VideoItem(uri, title));
                        }
                        Log.d(TAG, "播放列表加载成功，共" + playlistItems.size() + "个视频");
                    }
                } else {
                    // 如果没有传入播放列表，则创建只包含当前视频的播放列表
                    if (videoUri != null && videoTitle != null) {
                        playlistItems.add(new VideoAdapter.VideoItem(videoUri, videoTitle));
                        currentPlayingPosition = 0;
                        Log.d(TAG, "创建了单个视频的播放列表");
                    }
                }
            }
            
            // 如果Uri为空，则退出
            if (videoUri == null) {
                Toast.makeText(this, "无效的视频路径", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "无效的视频路径，退出播放器");
                finish();
                return;
            }
            
            // 初始化视图
            initializeViews();
            
            // 初始化播放器
            initializePlayer();
            
            // 设置监听器
            setupListeners();
            
            // 初始化播放列表
            setupPlaylist();
            
            // 开始自动隐藏控制栏的计时
            resetHideControlsTimer();
            
        } catch (Exception e) {
            Log.e(TAG, "初始化播放器失败: " + e.getMessage(), e);
            Toast.makeText(this, "播放器初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initializeViews() {
        playerView = findViewById(R.id.player_view);
        titleTextView = findViewById(R.id.video_title);
        currentTimeTextView = findViewById(R.id.current_time);
        durationTextView = findViewById(R.id.duration);
        seekBar = findViewById(R.id.seek_bar);
        playPauseButton = findViewById(R.id.btn_play_pause);
        previousButton = findViewById(R.id.btn_previous);
        nextButton = findViewById(R.id.btn_next);
        fullscreenButton = findViewById(R.id.btn_fullscreen);
        backButton = findViewById(R.id.btn_back);
        lockButton = findViewById(R.id.btn_lock);
        moreButton = findViewById(R.id.btn_more);
        playlistButton = findViewById(R.id.btn_playlist);
        topControls = findViewById(R.id.top_controls);
        bottomControls = findViewById(R.id.bottom_controls);
        playlistPanel = findViewById(R.id.playlist_panel);
        playlistPanelCard = findViewById(R.id.playlist_panel_card);
        playlistRecyclerView = findViewById(R.id.playlist_recycler_view);
        playlistEmptyView = findViewById(R.id.playlist_empty_view);
        
        // 设置视频标题 - 从URI获取实际文件名
        String actualFileName = getFileNameFromUri(videoUri);
        titleTextView.setText(actualFileName != null ? actualFileName : videoTitle);
        
        // 初始时间显示
        currentTimeTextView.setText(stringForTime(0));
        durationTextView.setText(stringForTime(0));
        
        // 设置进度条最大值为1000
        seekBar.setMax(1000);
    }
    
    // 从URI获取实际文件名
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        
        try {
            if (uri.getScheme() != null && uri.getScheme().equals("content")) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex);
                        }
                    }
                }
            }
            
            if (result == null) {
                result = uri.getLastPathSegment();
            }
            
            if (result == null) {
                result = "未知视频";
            }
        } catch (Exception e) {
            Log.e(TAG, "获取文件名失败", e);
            result = "未知视频";
        }
        
        return result;
    }
    
    private void setupPlaylist() {
        // 设置播放列表适配器
        playlistAdapter = new VideoAdapter();
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistRecyclerView.setAdapter(playlistAdapter);
        
        // 设置播放列表点击监听
        playlistAdapter.setOnVideoClickListener(videoItem -> {
            int position = -1;
            for (int i = 0; i < playlistItems.size(); i++) {
                if (playlistItems.get(i).getUri().equals(videoItem.getUri())) {
                    position = i;
                    break;
                }
            }
            if (position != -1 && position != currentPlayingPosition) {
                isUserSelect = true;
                currentPlayingPosition = position;
                playSelectedVideo();
                togglePlaylistPanel();
            }
        });
        
        // 更新播放列表
        updatePlaylist();
        
        // 根据播放列表项目数量调整控制按钮可见性
        updateNavigationButtonsVisibility();
    }
    
    private void updateNavigationButtonsVisibility() {
        boolean hasMultipleVideos = playlistItems.size() > 1;
        
        // 播放列表按钮
        playlistButton.setVisibility(hasMultipleVideos ? View.VISIBLE : View.GONE);
        
        // 上一个/下一个按钮
        previousButton.setVisibility(hasMultipleVideos ? View.VISIBLE : View.GONE);
        nextButton.setVisibility(hasMultipleVideos ? View.VISIBLE : View.GONE);
    }
    
    private void updatePlaylist() {
        if (playlistItems.isEmpty()) {
            playlistRecyclerView.setVisibility(View.GONE);
            playlistEmptyView.setVisibility(View.VISIBLE);
        } else {
            playlistRecyclerView.setVisibility(View.VISIBLE);
            playlistEmptyView.setVisibility(View.GONE);
            if (playlistAdapter != null) {
                playlistAdapter.setCurrentPlayingPosition(currentPlayingPosition);
                playlistAdapter.setVideoList(playlistItems);
            } else {
                Log.e(TAG, "播放列表适配器为空");
            }
        }
        
        // 更新导航按钮可见性
        updateNavigationButtonsVisibility();
    }
    
    private void playSelectedVideo() {
        try {
            if (currentPlayingPosition >= 0 && currentPlayingPosition < playlistItems.size()) {
                VideoAdapter.VideoItem item = playlistItems.get(currentPlayingPosition);
                videoUri = item.getUri();
                videoTitle = item.getDisplayName();
                if (videoUri == null) {
                    Log.e(TAG, "视频URI为空");
                    Toast.makeText(this, "无效的视频路径", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "准备播放视频: " + videoTitle + ", URI: " + videoUri);
                titleTextView.setText(videoTitle);
                MediaItem mediaItem = MediaItem.fromUri(videoUri);
                player.clearMediaItems();
                player.setMediaItem(mediaItem);
                player.prepare();
                player.play();
                isPlaying = true;
                try {
                    updatePlaylist();
                } catch (NullPointerException e) {
                    Log.e(TAG, "更新播放列表失败: " + e.getMessage(), e);
                }
                updateNavigationButtonsVisibility();
                Log.d(TAG, "视频加载成功，开始播放");
            } else {
                Log.e(TAG, "无效的播放位置: " + currentPlayingPosition);
                Toast.makeText(this, "无法播放选定的视频", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "播放视频时发生错误: " + e.getMessage(), e);
            Toast.makeText(this, "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void initializePlayer() {
        try {
            // 检查是否已经有播放器实例，如果有则释放
            if (player != null) {
                player.release();
            }
            
            // 创建ExoPlayer实例
            player = new ExoPlayer.Builder(this)
                    .setSeekBackIncrementMs(10000) // 设置快退增量为10秒
                    .setSeekForwardIncrementMs(10000) // 设置快进增量为10秒
                    .build();
            playerView.setPlayer(player);
            
            // 设置播放监听器
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        // 视频准备好了，更新UI
                        durationTextView.setText(stringForTime(player.getDuration()));
                        updateProgress();
                        handler.post(updateProgressRunnable);
                        Log.d(TAG, "播放器准备就绪");
                    } else if (state == Player.STATE_ENDED) {
                        // 根据设置决定是否自动播放下一集
                        if (autoPlayNext && !isUserNext && !isUserPrevious && !isUserSelect) {
                            playNextVideo();
                        }
                        isUserNext = false;
                        isUserPrevious = false;
                        isUserSelect = false;
                    } else if (state == Player.STATE_BUFFERING) {
                        Log.d(TAG, "视频缓冲中...");
                    } else if (state == Player.STATE_IDLE) {
                        Log.d(TAG, "播放器处于空闲状态");
                    }
                }
                
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    // 播放状态改变
                    PlayerActivity.this.isPlaying = isPlaying;
                    updatePlayPauseButton();
                    
                    if (isPlaying) {
                        handler.post(updateProgressRunnable);
                        Log.d(TAG, "视频开始播放");
                    } else {
                        handler.removeCallbacks(updateProgressRunnable);
                        Log.d(TAG, "视频暂停播放");
                    }
                }
                
                @Override
                public void onPlayerError(PlaybackException error) {
                    // 播放错误处理
                    Log.e(TAG, "播放错误: " + error.getMessage() + ", 错误代码: " + error.getErrorCodeName(), error);
                    Toast.makeText(PlayerActivity.this, "播放错误: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // 根据错误类型进行不同处理
                    String errorCode = error.getErrorCodeName();
                    if (errorCode.contains("IO_NETWORK_CONNECTION_FAILED")) {
                        Toast.makeText(PlayerActivity.this, "网络连接失败，请检查网络设置", Toast.LENGTH_SHORT).show();
                    } else if (errorCode.contains("IO_FILE_NOT_FOUND")) {
                        Toast.makeText(PlayerActivity.this, "找不到视频文件", Toast.LENGTH_SHORT).show();
                    } else {
                        // 尝试播放下一个视频
                        playNextVideo();
                    }
                }
            });
            
            // 播放当前选中的视频
            playSelectedVideo();
            
        } catch (Exception e) {
            Log.e(TAG, "初始化播放器时发生错误: " + e.getMessage(), e);
            Toast.makeText(this, "无法创建播放器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            throw e; // 重新抛出异常，让上层处理
        }
    }
    
    private void setupListeners() {
        // 播放/暂停按钮
        playPauseButton.setOnClickListener(v -> {
            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                    resetHideControlsTimer();
                }
            }
        });
        
        // 上一个视频按钮
        previousButton.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTimePrevious < DOUBLE_CLICK_TIME_DELTA) {
                // 双击 - 播放上一个视频
                playPreviousVideo();
                Toast.makeText(this, "播放上一个视频", Toast.LENGTH_SHORT).show();
            } else {
                // 单击 - 快退10秒
                if (player != null) {
                    long newPosition = player.getCurrentPosition() - 10000; // 快退10秒
                    if (newPosition < 0) newPosition = 0;
                    player.seekTo(newPosition);
                }
            }
            lastClickTimePrevious = clickTime;
            resetHideControlsTimer();
        });
        
        // 下一个视频按钮
        nextButton.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTimeNext < DOUBLE_CLICK_TIME_DELTA) {
                // 双击 - 播放下一个视频
                playNextVideo();
                Toast.makeText(this, "播放下一个视频", Toast.LENGTH_SHORT).show();
            } else {
                // 单击 - 快进10秒
                if (player != null) {
                    long newPosition = player.getCurrentPosition() + 10000; // 快进10秒
                    if (newPosition > player.getDuration()) newPosition = player.getDuration();
                    player.seekTo(newPosition);
                }
            }
            lastClickTimeNext = clickTime;
            resetHideControlsTimer();
        });
        
        // 全屏按钮
        fullscreenButton.setOnClickListener(v -> {
            toggleFullscreen();
            resetHideControlsTimer();
        });
        
        // 返回按钮
        backButton.setOnClickListener(v -> finish());
        
        // 锁定按钮
        lockButton.setOnClickListener(v -> {
            isLocked = !isLocked;
            // 更新锁定按钮图标
            lockButton.setImageResource(isLocked ? 
                    R.drawable.ic_player_lock : R.drawable.ic_player_unlock);
            if (isLocked) {
                topControls.setVisibility(View.GONE);
                bottomControls.setVisibility(View.GONE);
                lockButton.setVisibility(View.VISIBLE);
                addUnlockButtonToRoot();
            } else {
                showControls();
                resetHideControlsTimer();
                removeUnlockButtonFromRoot();
            }
        });
        
        // 更多按钮
        moreButton.setOnClickListener(v -> {
            // 显示更多选项菜单
            Toast.makeText(this, "更多功能开发中...", Toast.LENGTH_SHORT).show();
            resetHideControlsTimer();
        });
        
        // 播放列表按钮
        playlistButton.setOnClickListener(v -> {
            togglePlaylistPanel();
            resetHideControlsTimer();
        });
        
        // 点击播放器视图切换控制栏显示状态
        playerView.setOnClickListener(v -> {
            if (!isLocked) {
                if (controlsVisible) {
                    hideControls();
                } else {
                    showControls();
                    resetHideControlsTimer();
                }
            }
        });
        
        // 进度条监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    long position = duration * progress / 1000;
                    currentTimeTextView.setText(stringForTime(position));
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 拖动开始时暂停自动隐藏
                handler.removeCallbacks(hideControlsRunnable);
                handler.removeCallbacks(updateProgressRunnable);
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) {
                    long duration = player.getDuration();
                    long position = duration * seekBar.getProgress() / 1000;
                    player.seekTo(position);
                }
                
                // 拖动结束后恢复自动隐藏和进度更新
                handler.post(updateProgressRunnable);
                resetHideControlsTimer();
            }
        });
    }
    
    private void togglePlaylistPanel() {
        playlistVisible = !playlistVisible;
        
        if (playlistVisible) {
            playlistPanelCard.setVisibility(View.VISIBLE);
            updatePlaylist();
        } else {
            playlistPanelCard.setVisibility(View.GONE);
        }
    }
    
    private void playPreviousVideo() {
        isUserPrevious = true;
        if (playlistItems.size() > 1) {
            int previousPosition = (currentPlayingPosition - 1 + playlistItems.size()) % playlistItems.size();
            currentPlayingPosition = previousPosition;
            playSelectedVideo();
            updateNavigationButtonsVisibility();
        }
    }
    
    private void playNextVideo() {
        isUserNext = true;
        if (playlistItems.size() > 1) {
            int nextPosition = (currentPlayingPosition + 1) % playlistItems.size();
            currentPlayingPosition = nextPosition;
            playSelectedVideo();
            updateNavigationButtonsVisibility();
        }
    }
    
    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_player_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_player_play);
        }
    }
    
    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        
        if (isFullscreen) {
            // 横屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            fullscreenButton.setImageResource(R.drawable.ic_player_fullscreen_exit);
        } else {
            // 竖屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            fullscreenButton.setImageResource(R.drawable.ic_player_fullscreen);
        }
    }
    
    private void showControls() {
        if (!controlsVisible) {
            controlsVisible = true;
            
            if (!isLocked) {
                topControls.setVisibility(View.VISIBLE);
                bottomControls.setVisibility(View.VISIBLE);
            }
            
            lockButton.setVisibility(View.VISIBLE);
        }
    }
    
    private void hideControls() {
        if (controlsVisible && !playlistVisible) {
            controlsVisible = false;
            
            topControls.setVisibility(View.GONE);
            bottomControls.setVisibility(View.GONE);
            
            // 锁定按钮也隐藏，但在锁定状态下仍然可见
            if (!isLocked) {
                lockButton.setVisibility(View.GONE);
            }
        }
    }
    
    private void resetHideControlsTimer() {
        // 取消之前的定时器
        handler.removeCallbacks(hideControlsRunnable);
        
        // 如果正在播放，则设置新的定时器
        if (isPlaying && controlsVisible && !isLocked) {
            handler.postDelayed(hideControlsRunnable, 3000);
        }
    }
    
    private void updateProgress() {
        if (player != null && player.isPlaying()) {
            long position = player.getCurrentPosition();
            long duration = player.getDuration();
            
            if (duration > 0) {
                seekBar.setProgress((int) (1000 * position / duration));
                currentTimeTextView.setText(stringForTime(position));
            }
        }
    }
    
    private String stringForTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        formatBuilder.setLength(0);
        return formatter.format("%02d:%02d", minutes, seconds).toString();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
            isPlaying = false;
        }
        
        handler.removeCallbacks(updateProgressRunnable);
        handler.removeCallbacks(hideControlsRunnable);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && !isPlaying) {
            player.play();
            isPlaying = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 移除所有回调
        handler.removeCallbacks(hideControlsRunnable);
        handler.removeCallbacks(updateProgressRunnable);
        
        // 释放播放器资源
        if (player != null) {
            try {
                player.stop();
                player.clearMediaItems();
                player.release();
                player = null;
                Log.d(TAG, "播放器资源已释放");
            } catch (Exception e) {
                Log.e(TAG, "释放播放器资源时发生错误: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // 返回时同步播放状态
        super.onBackPressed();
    }

    // 动态添加解锁按钮到根布局
    private void addUnlockButtonToRoot() {
        if (unlockButton != null) return;
        unlockButton = new ImageButton(this);
        unlockButton.setId(View.generateViewId());
        unlockButton.setImageResource(R.drawable.ic_player_unlock);
        unlockButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        unlockButton.setContentDescription("解锁");
        unlockButton.setColorFilter(getResources().getColor(android.R.color.white));
        // 设置大小和位置（如左下角）
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(120, 120);
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.setMargins(32, 32, 32, 32);
        unlockButton.setLayoutParams(params);
        // 点击解锁
        unlockButton.setOnClickListener(v -> {
            isLocked = false;
            showControls();
            resetHideControlsTimer();
            removeUnlockButtonFromRoot();
        });
        // 添加到根布局
        ConstraintLayout root = findViewById(R.id.player_layout);
        root.addView(unlockButton);
    }

    // 移除解锁按钮
    private void removeUnlockButtonFromRoot() {
        if (unlockButton != null) {
            ConstraintLayout root = findViewById(R.id.player_layout);
            root.removeView(unlockButton);
            unlockButton = null;
        }
    }
}