package com.example.lplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import android.database.Cursor;
import android.provider.MediaStore;

public class MusicPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MusicPlayerActivity";
    
    private ExoPlayer player;
    private TextView titleTextView;
    private TextView artistTextView;
    private TextView currentTimeTextView;
    private TextView durationTextView;
    private SeekBar seekBar;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton backButton;
    private ImageButton repeatButton;
    private ImageButton shuffleButton;
    private ImageButton playlistButton;
    
    private ConstraintLayout topControls;
    private ConstraintLayout bottomControls;
    private ConstraintLayout playlistPanel;
    private androidx.cardview.widget.CardView playlistPanelCard;
    private RecyclerView playlistRecyclerView;
    private TextView playlistEmptyView;
    
    private String musicTitle;
    private String musicArtist;
    private Uri musicUri;
    private boolean isPlaying = false;
    private boolean isRepeatMode = false;
    private boolean isShuffleMode = false;
    private boolean playlistVisible = false;
    
    private List<MusicAdapter.MusicItem> playlistItems = new ArrayList<>();
    private MusicAdapter playlistAdapter;
    private int currentPlayingPosition = -1;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 1000);
        }
    };
    
    private StringBuilder formatBuilder;
    private Formatter formatter;
    
    // 标志位用于区分用户操作和自动播放结束
    private boolean isUserNext = false;
    private boolean isUserPrevious = false;
    private boolean isUserSelect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        
        try {
            // 从Intent获取数据
            if (getIntent() != null) {
                String uriString = getIntent().getStringExtra("music_uri");
                musicTitle = getIntent().getStringExtra("music_title");
                musicArtist = getIntent().getStringExtra("music_artist");
                
                if (uriString != null) {
                    musicUri = Uri.parse(uriString);
                    Log.d(TAG, "音乐URI: " + uriString);
                } else {
                    Log.e(TAG, "未提供音乐URI");
                }
                
                // 获取播放列表数据（如果有）
                if (getIntent().hasExtra("playlist")) {
                    ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("playlist");
                    ArrayList<String> titles = getIntent().getStringArrayListExtra("playlist_titles");
                    ArrayList<String> artists = getIntent().getStringArrayListExtra("playlist_artists");
                    ArrayList<String> albums = getIntent().getStringArrayListExtra("playlist_albums");
                    ArrayList<String> durations = getIntent().getStringArrayListExtra("playlist_durations");
                    currentPlayingPosition = getIntent().getIntExtra("current_position", 0);
                    
                    if (uriStrings != null && titles != null && artists != null && 
                            uriStrings.size() == titles.size() && titles.size() == artists.size()) {
                        for (int i = 0; i < uriStrings.size(); i++) {
                            Uri uri = Uri.parse(uriStrings.get(i));
                            String title = titles.get(i);
                            String artist = artists.get(i);
                            String album = albums != null && i < albums.size() ? albums.get(i) : "";
                            String duration = durations != null && i < durations.size() ? durations.get(i) : "";
                            playlistItems.add(new MusicAdapter.MusicItem(uri, title, artist, album, duration));
                        }
                        Log.d(TAG, "播放列表加载成功，共" + playlistItems.size() + "首音乐");
                    }
                } else {
                    // 如果没有传入播放列表，则创建只包含当前音乐的播放列表
                    if (musicUri != null && musicTitle != null) {
                        playlistItems.add(new MusicAdapter.MusicItem(musicUri, musicTitle, musicArtist, "", ""));
                        currentPlayingPosition = 0;
                        Log.d(TAG, "创建了单个音乐的播放列表");
                    }
                }
            }
            
            // 如果Uri为空，则退出
            if (musicUri == null) {
                Toast.makeText(this, "无效的音乐路径", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "无效的音乐路径，退出播放器");
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
            
        } catch (Exception e) {
            Log.e(TAG, "初始化播放器失败: " + e.getMessage(), e);
            Toast.makeText(this, "播放器初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initializeViews() {
        titleTextView = findViewById(R.id.music_title);
        artistTextView = findViewById(R.id.music_artist);
        currentTimeTextView = findViewById(R.id.current_time);
        durationTextView = findViewById(R.id.duration);
        seekBar = findViewById(R.id.seek_bar);
        playPauseButton = findViewById(R.id.btn_play_pause);
        previousButton = findViewById(R.id.btn_previous);
        nextButton = findViewById(R.id.btn_next);
        backButton = findViewById(R.id.btn_back);
        repeatButton = findViewById(R.id.btn_repeat);
        shuffleButton = findViewById(R.id.btn_shuffle);
        playlistButton = findViewById(R.id.btn_playlist);
        topControls = findViewById(R.id.top_controls);
        bottomControls = findViewById(R.id.bottom_controls);
        playlistPanel = findViewById(R.id.playlist_panel);
        playlistPanelCard = findViewById(R.id.playlist_panel_card);
        playlistRecyclerView = findViewById(R.id.playlist_recycler_view);
        playlistEmptyView = findViewById(R.id.playlist_empty_view);
        
        // 设置音乐标题和艺术家 - 从URI获取实际文件名
        String actualFileName = getFileNameFromUri(musicUri);
        titleTextView.setText(actualFileName != null ? actualFileName : musicTitle);
        artistTextView.setText(musicArtist != null ? musicArtist : "未知艺术家");
        
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
                result = "未知音乐";
            }
        } catch (Exception e) {
            Log.e(TAG, "获取文件名失败", e);
            result = "未知音乐";
        }
        
        return result;
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
            
            // 设置播放监听器
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        // 音乐准备好了，更新UI
                        durationTextView.setText(stringForTime(player.getDuration()));
                        updateProgress();
                        handler.post(updateProgressRunnable);
                        Log.d(TAG, "播放器准备就绪");
                    } else if (state == Player.STATE_ENDED) {
                        // 只有自然播放结束才自动切歌
                        if (!isUserNext && !isUserPrevious && !isUserSelect) {
                            playNextMusic();
                        }
                        isUserNext = false;
                        isUserPrevious = false;
                        isUserSelect = false;
                    } else if (state == Player.STATE_BUFFERING) {
                        Log.d(TAG, "音乐缓冲中...");
                    } else if (state == Player.STATE_IDLE) {
                        Log.d(TAG, "播放器处于空闲状态");
                    }
                }
                
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    // 播放状态改变
                    MusicPlayerActivity.this.isPlaying = isPlaying;
                    updatePlayPauseButton();
                    
                    if (isPlaying) {
                        handler.post(updateProgressRunnable);
                        Log.d(TAG, "音乐开始播放");
                    } else {
                        handler.removeCallbacks(updateProgressRunnable);
                        Log.d(TAG, "音乐暂停播放");
                    }
                }
                
                @Override
                public void onPlayerError(PlaybackException error) {
                    // 播放错误处理
                    Log.e(TAG, "播放错误: " + error.getMessage() + ", 错误代码: " + error.getErrorCodeName(), error);
                    Toast.makeText(MusicPlayerActivity.this, "播放错误: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    
                    // 根据错误类型进行不同处理
                    String errorCode = error.getErrorCodeName();
                    if (errorCode.contains("IO_NETWORK_CONNECTION_FAILED")) {
                        Toast.makeText(MusicPlayerActivity.this, "网络连接失败，请检查网络设置", Toast.LENGTH_SHORT).show();
                    } else if (errorCode.contains("IO_FILE_NOT_FOUND")) {
                        Toast.makeText(MusicPlayerActivity.this, "找不到音乐文件", Toast.LENGTH_SHORT).show();
                    } else {
                        // 尝试播放下一首音乐
                        playNextMusic();
                    }
                }
            });
            
            // 播放当前选中的音乐
            playSelectedMusic();
            
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
                }
            }
        });
        
        // 上一首按钮
        previousButton.setOnClickListener(v -> playPreviousMusic());
        
        // 下一首按钮
        nextButton.setOnClickListener(v -> playNextMusic());
        
        // 返回按钮
        backButton.setOnClickListener(v -> finish());
        
        // 重复模式按钮
        repeatButton.setOnClickListener(v -> {
            isRepeatMode = !isRepeatMode;
            updateRepeatButton();
            Toast.makeText(this, isRepeatMode ? "单曲循环已开启" : "单曲循环已关闭", Toast.LENGTH_SHORT).show();
        });
        
        // 随机播放按钮
        shuffleButton.setOnClickListener(v -> {
            isShuffleMode = !isShuffleMode;
            updateShuffleButton();
            Toast.makeText(this, isShuffleMode ? "随机播放已开启" : "随机播放已关闭", Toast.LENGTH_SHORT).show();
        });
        
        // 播放列表按钮
        playlistButton.setOnClickListener(v -> togglePlaylistPanel());
        
        // 进度条监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    long duration = player.getDuration();
                    long newPosition = (duration * progress) / 1000;
                    currentTimeTextView.setText(stringForTime(newPosition));
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始拖动时暂停进度更新
                handler.removeCallbacks(updateProgressRunnable);
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) {
                    long duration = player.getDuration();
                    long newPosition = (duration * seekBar.getProgress()) / 1000;
                    player.seekTo(newPosition);
                }
                // 恢复进度更新
                handler.post(updateProgressRunnable);
            }
        });
    }
    
    private void setupPlaylist() {
        // 设置播放列表适配器
        playlistAdapter = new MusicAdapter();
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistRecyclerView.setAdapter(playlistAdapter);
        
        // 设置播放列表点击监听
        playlistAdapter.setOnMusicClickListener(musicItem -> {
            int position = -1;
            for (int i = 0; i < playlistItems.size(); i++) {
                if (playlistItems.get(i).getUri().equals(musicItem.getUri())) {
                    position = i;
                    break;
                }
            }
            if (position != -1 && position != currentPlayingPosition) {
                isUserSelect = true;
                currentPlayingPosition = position;
                playSelectedMusic();
                togglePlaylistPanel();
            }
        });
        
        // 更新播放列表
        updatePlaylist();
        
        // 根据播放列表项目数量调整控制按钮可见性
        updateNavigationButtonsVisibility();
    }
    
    private void updateNavigationButtonsVisibility() {
        boolean hasMultipleMusics = playlistItems.size() > 1;
        
        // 播放列表按钮
        playlistButton.setVisibility(hasMultipleMusics ? View.VISIBLE : View.GONE);
        
        // 上一个/下一个按钮
        previousButton.setVisibility(hasMultipleMusics ? View.VISIBLE : View.GONE);
        nextButton.setVisibility(hasMultipleMusics ? View.VISIBLE : View.GONE);
    }
    
    private void updatePlaylist() {
        if (playlistItems.isEmpty()) {
            playlistRecyclerView.setVisibility(View.GONE);
            playlistEmptyView.setVisibility(View.VISIBLE);
        } else {
            playlistRecyclerView.setVisibility(View.VISIBLE);
            playlistEmptyView.setVisibility(View.GONE);
            if (playlistAdapter != null) {
                // 先设置当前播放位置，再设置播放列表，避免UI跳跃
                playlistAdapter.setCurrentPlayingPosition(currentPlayingPosition);
                playlistAdapter.setMusicList(playlistItems);
            } else {
                Log.e(TAG, "播放列表适配器为空");
            }
        }
        
        // 更新导航按钮可见性
        updateNavigationButtonsVisibility();
    }
    
    private void playSelectedMusic() {
        try {
            if (currentPlayingPosition >= 0 && currentPlayingPosition < playlistItems.size()) {
                MusicAdapter.MusicItem item = playlistItems.get(currentPlayingPosition);
                musicUri = item.getUri();
                musicTitle = item.getDisplayName();
                musicArtist = item.getArtist();
                
                // 检查URI是否有效
                if (musicUri == null) {
                    Log.e(TAG, "音乐URI为空");
                    Toast.makeText(this, "无效的音乐路径", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Log.d(TAG, "准备播放音乐: " + musicTitle + ", URI: " + musicUri);
                
                // 更新标题和艺术家
                titleTextView.setText(musicTitle);
                artistTextView.setText(musicArtist != null ? musicArtist : "未知艺术家");
                
                // 准备媒体项
                MediaItem mediaItem = MediaItem.fromUri(musicUri);
                
                // 重置播放器
                player.clearMediaItems();
                player.setMediaItem(mediaItem);
                
                // 准备播放
                player.prepare();
                player.play();
                isPlaying = true;
                
                try {
                    // 更新播放列表UI
                    updatePlaylist();
                } catch (NullPointerException e) {
                    Log.e(TAG, "更新播放列表失败: " + e.getMessage(), e);
                }
                
                // 更新导航按钮状态
                updateNavigationButtonsVisibility();
                
                Log.d(TAG, "音乐加载成功，开始播放");
            } else {
                Log.e(TAG, "无效的播放位置: " + currentPlayingPosition);
                Toast.makeText(this, "无法播放选定的音乐", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "播放音乐时发生错误: " + e.getMessage(), e);
            Toast.makeText(this, "播放失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void playNextMusic() {
        isUserNext = true;
        if (playlistItems.size() > 1) {
            int nextPosition = (currentPlayingPosition + 1) % playlistItems.size();
            currentPlayingPosition = nextPosition;
            playSelectedMusic();
            updateNavigationButtonsVisibility();
        }
    }
    
    private void playPreviousMusic() {
        isUserPrevious = true;
        if (playlistItems.size() > 1) {
            int prevPosition = (currentPlayingPosition - 1 + playlistItems.size()) % playlistItems.size();
            currentPlayingPosition = prevPosition;
            playSelectedMusic();
            updateNavigationButtonsVisibility();
        }
    }
    
    private void togglePlaylistPanel() {
        playlistVisible = !playlistVisible;
        playlistPanelCard.setVisibility(playlistVisible ? View.VISIBLE : View.GONE);
    }
    
    private void updateProgress() {
        if (player != null) {
            long duration = player.getDuration();
            long position = player.getCurrentPosition();
            
            if (duration > 0) {
                // 更新进度条
                int progress = (int) (1000 * position / duration);
                seekBar.setProgress(progress);
                
                // 更新时间显示
                currentTimeTextView.setText(stringForTime(position));
                durationTextView.setText(stringForTime(duration));
            }
        }
    }
    
    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_player_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_player_play);
        }
    }
    
    private void updateRepeatButton() {
        if (isRepeatMode) {
            repeatButton.setImageResource(R.drawable.ic_repeat_one);
        } else {
            repeatButton.setImageResource(R.drawable.ic_repeat);
        }
    }
    
    private void updateShuffleButton() {
        if (isShuffleMode) {
            shuffleButton.setImageResource(R.drawable.ic_shuffle_on);
        } else {
            shuffleButton.setImageResource(R.drawable.ic_shuffle);
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
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && !player.isPlaying()) {
            player.play();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        handler.removeCallbacks(updateProgressRunnable);
    }
    
    @Override
    public void onBackPressed() {
        if (playlistVisible) {
            togglePlaylistPanel();
        } else {
            super.onBackPressed();
        }
    }
}