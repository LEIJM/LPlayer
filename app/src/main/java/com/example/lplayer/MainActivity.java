package com.example.lplayer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.lplayer.fragments.MusicFragment;
import com.example.lplayer.fragments.PlaylistFragment;
import com.example.lplayer.fragments.SettingsFragment;
import com.example.lplayer.fragments.VideoFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.preference.PreferenceManager;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.media.MediaMetadataRetriever;

public class MainActivity extends AppCompatActivity 
        implements VideoFragment.VideoFragmentListener, 
                   PlaylistFragment.PlaylistFragmentListener,
                   MusicFragment.MusicFragmentListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    // 用于跟踪权限请求的来源
    private static final int REQUEST_ACTION_VIDEO = 1;
    private static final int REQUEST_ACTION_FOLDER = 2;
    private int lastRequestedAction = 0;

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private ViewPagerAdapter viewPagerAdapter;
    private Toolbar toolbar;
    private FrameLayout settingsContainer;
    private boolean isSettingsVisible = false;
    
    private List<VideoAdapter.VideoItem> videoList = new ArrayList<>();
    private int currentPlayingPosition = -1;
    
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isRefreshing = false;
    
    // 保存最后选择的文件夹URI
    private Uri lastSelectedFolderUri = null;

    // 视频选择器
    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedVideoUri = result.getData().getData();
                    if (selectedVideoUri != null) {
                        try {
                            // 清空当前视频列表
                            videoList.clear();
                            // 只添加当前选择的视频
                            String name = getFileNameFromUri(selectedVideoUri);
                            VideoAdapter.VideoItem videoItem = new VideoAdapter.VideoItem(selectedVideoUri, name);
                            videoList.add(videoItem);
                            updateVideoLists();
                            playVideo(videoItem);
                        } catch (Exception e) {
                            Log.e(TAG, "播放视频失败", e);
                            Toast.makeText(this, "无法播放选定的视频", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            
    // 文件夹选择器
    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri folderUri = result.getData().getData();
                    if (folderUri != null) {
                        try {
                            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(folderUri, takeFlags);
                            // 保存最后选择的文件夹
                            lastSelectedFolderUri = folderUri;
                            loadVideosFromFolder(folderUri);
                        } catch (Exception e) {
                            Log.e(TAG, "获取文件夹权限失败", e);
                            Toast.makeText(this, "无法访问选定的文件夹", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            
    // 视频播放器
    private final ActivityResultLauncher<Intent> playerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    // 1. 同步播放状态和播放列表
                    if (data.hasExtra("current_position")) {
                        int pos = data.getIntExtra("current_position", -1);
                        if (pos >= 0 && pos < videoList.size()) {
                            currentPlayingPosition = pos;
                        }
                    }
                    if (data.hasExtra("playlist") && data.hasExtra("playlist_titles")) {
                        ArrayList<String> uriStrings = data.getStringArrayListExtra("playlist");
                        ArrayList<String> titles = data.getStringArrayListExtra("playlist_titles");
                        if (uriStrings != null && titles != null && uriStrings.size() == titles.size()) {
                            videoList.clear();
                            for (int i = 0; i < uriStrings.size(); i++) {
                                videoList.add(new VideoAdapter.VideoItem(Uri.parse(uriStrings.get(i)), titles.get(i)));
                            }
                        }
                    }
                    updateVideoLists();
                    String action = data.getStringExtra("action");
                    if (data.getAction() != null && data.getAction().equals("com.example.lplayer.REQUEST_VIDEO_LIST")) {
                        // ... existing code ...
                    } else if (action != null) {
                        // ... existing code ...
                    }
                }
            });

    // 音乐文件夹选择器
    private final ActivityResultLauncher<Intent> musicFolderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri folderUri = result.getData().getData();
                    if (folderUri != null) {
                        try {
                            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(folderUri, takeFlags);
                            
                            // 保存为默认音乐文件夹
                            PreferenceManager.getDefaultSharedPreferences(this)
                                    .edit()
                                    .putString("default_music_folder_uri", folderUri.toString())
                                    .apply();
                            
                            // 加载音乐文件
                            loadMusicFromFolder(folderUri);
                            
                            Toast.makeText(this, "已设置默认音乐文件夹", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, "获取音乐文件夹权限失败", e);
                            Toast.makeText(this, "无法访问选定的音乐文件夹", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // 初始化视图
            viewPager = findViewById(R.id.viewPager);
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            toolbar = findViewById(R.id.toolbar);
            settingsContainer = findViewById(R.id.settings_container);
            swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
            
            // 设置Toolbar为ActionBar
            setSupportActionBar(toolbar);
            
            // 设置ViewPager
            setupViewPager();
            
            // 设置底部导航
            setupBottomNavigation();

            // 设置下拉刷新
            setupSwipeRefresh();

            // 首次启动时加载默认文件夹内容
            if (savedInstanceState == null) {
                loadDefaultFolders();
            }

            // 根据设置更新底部导航栏
            updateBottomNavigation();
        } catch (Exception e) {
            Log.e(TAG, "初始化界面失败", e);
            Toast.makeText(this, "应用初始化失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.menu_add) {
            // 根据当前所在的界面来决定点击加号按钮的行为
            int currentPosition = viewPager.getCurrentItem();
            int[] enabledTabs = viewPagerAdapter.getEnabledTabs();
            if (currentPosition >= 0 && currentPosition < enabledTabs.length) {
                int tabType = enabledTabs[currentPosition];
                if (tabType == ViewPagerAdapter.TAB_VIDEO) {
                    // 在视频界面，选择视频文件夹
                    checkPermissionAndPickFolder();
                } else if (tabType == ViewPagerAdapter.TAB_MUSIC) {
                    // 在音乐界面，选择音乐文件夹
                    checkPermissionAndPickMusicFolder();
                } else {
                    // 默认行为，选择视频文件夹
                    checkPermissionAndPickFolder();
                }
            }
            return true;
        } else if (id == R.id.menu_settings) {
            toggleSettings();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void setupViewPager() {
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);
        
        // 禁用ViewPager滑动
        viewPager.setUserInputEnabled(false);
        
        // 设置页面切换监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomNavigationView.setSelectedItemId(getMenuItemIdForPosition(position));
                
                // 当切换到音乐界面时
                if (viewPagerAdapter.getEnabledTabs()[position] == ViewPagerAdapter.TAB_MUSIC) {
                    String defaultMusicFolderUri = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                            .getString("default_music_folder_uri", null);
                    if (defaultMusicFolderUri != null) {
                        Uri musicFolderUri = Uri.parse(defaultMusicFolderUri);
                        // 验证权限
                        List<UriPermission> permissions = getContentResolver().getPersistedUriPermissions();
                        boolean hasPermission = false;
                        for (UriPermission permission : permissions) {
                            if (permission.getUri().equals(musicFolderUri) && 
                                permission.isReadPermission()) {
                                hasPermission = true;
                                break;
                            }
                        }
                        
                        if (hasPermission) {
                            loadMusicFromFolder(musicFolderUri);
                        } else {
                            // 如果没有权限，尝试重新获取
                            try {
                                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                                getContentResolver().takePersistableUriPermission(musicFolderUri, takeFlags);
                                loadMusicFromFolder(musicFolderUri);
                            } catch (SecurityException e) {
                                Log.e(TAG, "无法获取音乐文件夹权限", e);
                                // 清除无效的文件夹设置
                                PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                                        .edit()
                                        .remove("default_music_folder_uri")
                                        .apply();
                                
                                // 显示提示信息
                                Fragment musicFragment = getSupportFragmentManager()
                                        .findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
                                if (musicFragment instanceof MusicFragment) {
                                    ((MusicFragment) musicFragment).showEmptyView("请在设置中选择音乐文件夹");
                                }
                                
                                Toast.makeText(MainActivity.this, "无法访问音乐文件夹，请重新选择", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        // 如果没有设置音乐文件夹，显示提示信息
                        Fragment musicFragment = getSupportFragmentManager()
                                .findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
                        if (musicFragment instanceof MusicFragment) {
                            ((MusicFragment) musicFragment).showEmptyView("请在设置中选择音乐文件夹");
                        }
                    }
                }
            }
        });
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int position;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_video) {
                position = ViewPagerAdapter.TAB_VIDEO;
            } else if (itemId == R.id.nav_music) {
                position = ViewPagerAdapter.TAB_MUSIC;
            } else if (itemId == R.id.nav_playlist) {
                position = ViewPagerAdapter.TAB_PLAYLIST;
            } else {
                return false;
            }
            
            viewPager.setCurrentItem(position, false);
            return true;
        });
    }
    
    private int getMenuItemIdForPosition(int position) {
        // 根据实际启用的标签页返回对应的菜单项ID
        int[] enabledTabs = viewPagerAdapter.getEnabledTabs();
        if (position >= 0 && position < enabledTabs.length) {
            switch (enabledTabs[position]) {
                case ViewPagerAdapter.TAB_VIDEO:
                    return R.id.nav_video;
                case ViewPagerAdapter.TAB_MUSIC:
                    return R.id.nav_music;
                case ViewPagerAdapter.TAB_PLAYLIST:
                    return R.id.nav_playlist;
            }
        }
        return R.id.nav_video; // 默认返回视频标签页
    }
    
    // VideoFragmentListener 方法实现
    @Override
    public void onVideoItemClicked(VideoAdapter.VideoItem videoItem) {
        playVideo(videoItem);
    }
    
    // PlaylistFragmentListener 方法实现
    @Override
    public void onPlaylistItemClicked(VideoAdapter.VideoItem videoItem) {
        playVideo(videoItem);
    }
    
    // MusicFragmentListener 方法实现
    @Override
    public void onMusicItemClicked(MusicAdapter.MusicItem musicItem) {
        playMusic(musicItem);
    }
    
    private void playMusic(MusicAdapter.MusicItem musicItem) {
        try {
            if (musicItem != null && musicItem.getUri() != null) {
                // 启动音乐播放器
                Intent intent = new Intent(this, MusicPlayerActivity.class);
                intent.putExtra("music_uri", musicItem.getUri().toString());
                intent.putExtra("music_title", musicItem.getDisplayName());
                intent.putExtra("music_artist", musicItem.getArtist());
                
                // 获取当前的MusicFragment
                Fragment musicFragment = getSupportFragmentManager().findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
                if (musicFragment instanceof MusicFragment) {
                    // 构建播放列表数据
                    List<MusicAdapter.MusicItem> musicList = ((MusicFragment) musicFragment).getMusicList();
                    if (musicList != null && !musicList.isEmpty()) {
                        // 查找当前音乐在列表中的位置
                        int position = -1;
                        for (int i = 0; i < musicList.size(); i++) {
                            if (musicList.get(i).getUri().equals(musicItem.getUri())) {
                                position = i;
                                break;
                            }
                        }
                        
                        if (position != -1) {
                            intent.putExtra("current_position", position);
                        }
                    }
                }
                
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "播放音乐失败", e);
            Toast.makeText(this, "无法播放选定的音乐: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkPermissionAndPickVideo() {
        lastRequestedAction = REQUEST_ACTION_VIDEO;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13及以上使用READ_MEDIA_VIDEO权限
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                            PERMISSION_REQUEST_CODE);
                } else {
                    openVideoPicker();
                }
            } else {
                // Android 12及以下使用READ_EXTERNAL_STORAGE权限
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                } else {
                    openVideoPicker();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查权限和选择视频失败", e);
            Toast.makeText(this, "无法选择视频", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkPermissionAndPickFolder() {
        lastRequestedAction = REQUEST_ACTION_FOLDER;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                            PERMISSION_REQUEST_CODE);
                } else {
                    openFolderPicker();
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                } else {
                    openFolderPicker();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查权限和选择文件夹失败", e);
            Toast.makeText(this, "无法选择文件夹", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkPermissionAndPickMusicFolder() {
        lastRequestedAction = REQUEST_ACTION_FOLDER;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                            PERMISSION_REQUEST_CODE);
                } else {
                    openMusicFolderPicker();
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                } else {
                    openMusicFolderPicker();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查权限和选择音乐文件夹失败", e);
            Toast.makeText(this, "无法选择音乐文件夹", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openVideoPicker() {
        try {
            // 检查是否有默认视频文件夹
            String defaultFolderUri = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("default_video_folder_uri", null);
            
            if (defaultFolderUri != null) {
                // 如果有默认文件夹，直接打开该文件夹
                Uri folderUri = Uri.parse(defaultFolderUri);
                // 保存最后选择的文件夹
                lastSelectedFolderUri = folderUri;
                // 清空当前视频列表
                videoList.clear();
                updateVideoLists();
                // 加载新文件夹内容
                loadVideosFromFolder(folderUri);
            } else {
                // 否则打开系统文件选择器
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                videoPickerLauncher.launch(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "打开视频选择器失败", e);
            Toast.makeText(this, "无法打开视频选择器", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openFolderPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            folderPickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "打开文件夹选择器失败", e);
            Toast.makeText(this, "无法打开文件夹选择器", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openMusicFolderPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            musicFolderPickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "打开音乐文件夹选择器失败", e);
            Toast.makeText(this, "无法打开音乐文件夹选择器", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isRefreshing) {
                isRefreshing = true;
                refreshContent();
            }
        });
    }

    private void refreshContent() {
        try {
            // 根据当前页面刷新内容
            int currentPosition = viewPager.getCurrentItem();
            switch (currentPosition) {
                case ViewPagerAdapter.TAB_VIDEO:
                    // 优先使用最后选择的文件夹
                    if (lastSelectedFolderUri != null) {
                        loadVideosFromFolder(lastSelectedFolderUri);
                    } else {
                        // 如果没有最后选择的文件夹，则使用默认文件夹
                        String defaultVideoFolderUri = PreferenceManager.getDefaultSharedPreferences(this)
                                .getString("default_video_folder_uri", null);
                        if (defaultVideoFolderUri != null) {
                            Uri videoFolderUri = Uri.parse(defaultVideoFolderUri);
                            lastSelectedFolderUri = videoFolderUri; // 更新最后选择的文件夹
                            loadVideosFromFolder(videoFolderUri);
                        } else {
                            // 如果没有默认文件夹，清空列表
                            videoList.clear();
                            updateVideoLists();
                            Toast.makeText(this, "请先选择视频文件夹", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;

                case ViewPagerAdapter.TAB_MUSIC:
                    // 刷新音乐列表
                    String defaultMusicFolderUri = PreferenceManager.getDefaultSharedPreferences(this)
                            .getString("default_music_folder_uri", null);
                    if (defaultMusicFolderUri != null) {
                        Uri musicFolderUri = Uri.parse(defaultMusicFolderUri);
                        loadMusicFromFolder(musicFolderUri);
                    } else {
                        // 如果没有默认文件夹，清空列表
                        Fragment musicFragment = getSupportFragmentManager()
                                .findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
                        if (musicFragment instanceof MusicFragment) {
                            ((MusicFragment) musicFragment).updateMusicList(new ArrayList<>());
                            Toast.makeText(this, "请先设置默认音乐文件夹", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;

                case ViewPagerAdapter.TAB_PLAYLIST:
                    // 播放列表会自动更新，因为它使用的是视频列表的数据
                    updateVideoLists();
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "刷新内容失败", e);
            Toast.makeText(this, "刷新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            // 延迟关闭刷新动画，给用户更好的视觉反馈
            swipeRefreshLayout.postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
                isRefreshing = false;
            }, 500);
        }
    }
    
    private void loadVideosFromFolder(Uri folderUri) {
        try {
            Log.d(TAG, "开始扫描文件夹: " + folderUri);
            
            // 创建独立线程处理文件扫描，避免阻塞UI线程
            new Thread(() -> {
                List<VideoAdapter.VideoItem> newVideos = new ArrayList<>();
                String folderName = null;
                
                try {
                    DocumentFile folder = DocumentFile.fromTreeUri(this, folderUri);
                    
                    if (folder != null && folder.exists()) {
                        folderName = folder.getName();
                        Log.d(TAG, "文件夹存在: " + folderName);
                        DocumentFile[] files = folder.listFiles();
                        Log.d(TAG, "找到文件数量: " + files.length);
                        
                        // 遍历查找视频文件
                        for (DocumentFile file : files) {
                            try {
                                if (file != null && file.isFile() && file.getType() != null && file.getType().startsWith("video/")) {
                                    String name = file.getName();
                                    Uri uri = file.getUri();
                                    if (name != null && uri != null) {
                                        Log.d(TAG, "找到视频: " + name + ", URI: " + uri);
                                        newVideos.add(new VideoAdapter.VideoItem(uri, name));
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "处理单个文件时出错", e);
                            }
                        }
                    } else {
                        Log.e(TAG, "文件夹不存在或无法访问");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "扫描文件夹失败", e);
                }
                
                // 对视频列表进行排序（按名称字母顺序）
                sortVideoList(newVideos);
                
                Log.d(TAG, "找到视频文件数量: " + newVideos.size());
                
                // 在UI线程更新界面
                final String finalFolderName = folderName;
                runOnUiThread(() -> {
                    try {
                        // 清空当前视频列表
                        videoList.clear();
                        
                        if (newVideos.isEmpty()) {
                            Toast.makeText(MainActivity.this, R.string.no_videos_found, Toast.LENGTH_SHORT).show();
                        } else {
                            // 直接替换视频列表
                            videoList.addAll(newVideos);
                            Toast.makeText(MainActivity.this, 
                                    String.format("已加载 %d 个视频", newVideos.size()), 
                                    Toast.LENGTH_SHORT).show();
                        }
                        
                        // 更新UI，但不切换导航
                        updateVideoLists();
                        
                        // 确保标题栏显示应用名称，并禁用返回箭头
                        if (getSupportActionBar() != null && !isSettingsVisible) {
                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                            getSupportActionBar().setTitle(R.string.app_name);
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "更新UI失败", e);
                        Toast.makeText(MainActivity.this, "加载视频列表失败", Toast.LENGTH_SHORT).show();
                    } finally {
                        // 确保刷新动画被关闭
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                            isRefreshing = false;
                        }
                    }
                });
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "加载文件夹中的视频失败", e);
            Toast.makeText(this, "无法加载文件夹中的视频", Toast.LENGTH_SHORT).show();
            // 确保刷新动画被关闭
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
                isRefreshing = false;
            }
        }
    }
    
    /**
     * 对视频列表按名称进行排序
     */
    private void sortVideoList(List<VideoAdapter.VideoItem> videos) {
        if (videos != null && !videos.isEmpty()) {
            videos.sort((v1, v2) -> {
                if (v1 == null || v1.getDisplayName() == null) return 1;
                if (v2 == null || v2.getDisplayName() == null) return -1;
                return v1.getDisplayName().toLowerCase().compareTo(v2.getDisplayName().toLowerCase());
            });
        }
    }
    
    private void updateVideoLists() {
        try {
            // 更新视频页面
            Fragment videoFragment = getSupportFragmentManager().findFragmentByTag("f" + ViewPagerAdapter.TAB_VIDEO);
            if (videoFragment instanceof VideoFragment) {
                ((VideoFragment) videoFragment).setVideos(videoList);
                ((VideoFragment) videoFragment).setCurrentPlayingPosition(currentPlayingPosition);
            }
            
            // 更新播放列表页面
            Fragment playlistFragment = getSupportFragmentManager().findFragmentByTag("f" + ViewPagerAdapter.TAB_PLAYLIST);
            if (playlistFragment instanceof PlaylistFragment) {
                ((PlaylistFragment) playlistFragment).setVideos(videoList);
                ((PlaylistFragment) playlistFragment).setCurrentPlayingPosition(currentPlayingPosition);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新视频列表失败", e);
        }
    }
    
    private void playVideo(VideoAdapter.VideoItem videoItem) {
        try {
            if (videoItem != null && videoItem.getUri() != null) {
                for (int i = 0; i < videoList.size(); i++) {
                    if (videoList.get(i).getUri().equals(videoItem.getUri())) {
                        currentPlayingPosition = i;
                        break;
                    }
                }
                updateVideoLists();
                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("video_uri", videoItem.getUri().toString());
                intent.putExtra("video_title", videoItem.getDisplayName());
                ArrayList<String> uriStrings = new ArrayList<>();
                ArrayList<String> titles = new ArrayList<>();
                for (VideoAdapter.VideoItem item : videoList) {
                    uriStrings.add(item.getUri().toString());
                    titles.add(item.getDisplayName());
                }
                intent.putStringArrayListExtra("playlist", uriStrings);
                intent.putStringArrayListExtra("playlist_titles", titles);
                intent.putExtra("current_position", currentPlayingPosition);
                playerLauncher.launch(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "播放视频失败", e);
            Toast.makeText(this, "无法播放选定的视频", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void playPreviousVideo() {
        try {
            if (videoList.isEmpty()) {
                return;
            }
            int newPosition = currentPlayingPosition - 1;
            if (newPosition < 0) {
                newPosition = videoList.size() - 1;
            }
            currentPlayingPosition = newPosition;
            playVideo(videoList.get(newPosition));
        } catch (Exception e) {
            Log.e(TAG, "播放上一个视频失败", e);
            Toast.makeText(this, "无法播放上一个视频", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void playNextVideo() {
        try {
            if (videoList.isEmpty()) {
                return;
            }
            int newPosition = currentPlayingPosition + 1;
            if (newPosition >= videoList.size()) {
                newPosition = 0;
            }
            currentPlayingPosition = newPosition;
            playVideo(videoList.get(newPosition));
        } catch (Exception e) {
            Log.e(TAG, "播放下一个视频失败", e);
            Toast.makeText(this, "无法播放下一个视频", Toast.LENGTH_SHORT).show();
        }
    }
    
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
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            if (requestCode == PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 根据请求的权限确定要打开的选择器
                    if (permissions[0].equals(Manifest.permission.READ_MEDIA_VIDEO) || 
                        permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // 检查选择的哪个菜单项触发了权限请求
                        if (lastRequestedAction == REQUEST_ACTION_VIDEO) {
                            openVideoPicker();
                        } else if (lastRequestedAction == REQUEST_ACTION_FOLDER) {
                            openFolderPicker();
                        }
                    } else if (permissions[0].equals(Manifest.permission.READ_MEDIA_AUDIO)) {
                        // 音乐文件夹权限
                        openMusicFolderPicker();
                    }
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "权限请求结果处理失败", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 只更新视频列表，不重新加载文件夹
        updateVideoLists();
    }

    private void toggleSettings() {
        if (!isSettingsVisible) {
            showSettings();
        } else {
            closeSettings();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (isSettingsVisible) {
            closeSettings();
            return true;
        }
        return super.onSupportNavigateUp();
    }

    public void showSettings() {
        if (!isSettingsVisible) {
            // 隐藏主内容
            viewPager.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(false); // 禁用下拉刷新
            
            // 显示设置容器
            settingsContainer.setVisibility(View.VISIBLE);
            
            // 添加设置Fragment（如果还没有添加）
            if (settingsContainer.getChildCount() == 0) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.settings_container, new SettingsFragment())
                    .commit();
            }
            
            // 更新Toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("设置");
            }
            
            isSettingsVisible = true;
        }
    }

    public void closeSettings() {
        if (isSettingsVisible) {
            // 清空Fragment返回栈
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            
            // 隐藏设置容器
            settingsContainer.setVisibility(View.GONE);
            
            // 显示主内容
            viewPager.setVisibility(View.VISIBLE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setEnabled(true); // 启用下拉刷新
            
            // 更新Toolbar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(R.string.app_name);
            }
            
            isSettingsVisible = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        try {
            // 保存视频播放列表
            PlaylistManager.saveVideoPlaylist(this, videoList);
            
            // 保存音乐播放列表
            Fragment musicFragment = getSupportFragmentManager().findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
            if (musicFragment instanceof MusicFragment) {
                List<MusicAdapter.MusicItem> musicList = ((MusicFragment) musicFragment).getMusicList();
                PlaylistManager.saveMusicPlaylist(this, musicList);
            }
            
            Log.d(TAG, "播放列表已保存");
        } catch (Exception e) {
            Log.e(TAG, "保存播放列表失败: " + e.getMessage(), e);
        }
    }

    private void loadDefaultFolders() {
        try {
            // 检查是否启用了保存播放列表功能
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean saveVideoPlaylist = prefs.getBoolean("save_video_playlist", false);
            boolean saveMusicPlaylist = prefs.getBoolean("save_music_playlist", false);
            
            if (saveVideoPlaylist) {
                // 如果启用了视频播放列表保存功能，尝试加载保存的播放列表
                List<VideoAdapter.VideoItem> savedVideoList = PlaylistManager.loadVideoPlaylist(this);
                if (!savedVideoList.isEmpty()) {
                    // 如果有保存的播放列表，直接使用它
                    videoList.clear();
                    videoList.addAll(savedVideoList);
                    updateVideoLists();
                    Log.d(TAG, "已加载保存的视频播放列表，共 " + videoList.size() + " 项");
                    return;
                }
            }
            
            // 如果没有保存的播放列表或未启用保存功能，则加载默认视频文件夹
            String defaultVideoFolderUri = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("default_video_folder_uri", null);
            if (defaultVideoFolderUri != null) {
                Uri videoFolderUri = Uri.parse(defaultVideoFolderUri);
                // 保存最后选择的文件夹
                lastSelectedFolderUri = videoFolderUri;
                // 确保有权限访问该文件夹
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                try {
                    getContentResolver().takePersistableUriPermission(videoFolderUri, takeFlags);
                    loadVideosFromFolder(videoFolderUri);
                } catch (SecurityException e) {
                    Log.e(TAG, "无法获取视频文件夹权限", e);
                    // 清除无效的文件夹设置
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .remove("default_video_folder_uri")
                            .apply();
                    lastSelectedFolderUri = null; // 清除最后选择的文件夹
                    // 清空视频列表
                    videoList.clear();
                    updateVideoLists();
                }
            } else {
                // 如果没有默认文件夹，清空视频列表
                videoList.clear();
                updateVideoLists();
            }

            // 处理音乐播放列表
            if (saveMusicPlaylist) {
                // 如果启用了音乐播放列表保存功能，尝试加载保存的播放列表
                List<MusicAdapter.MusicItem> savedMusicList = PlaylistManager.loadMusicPlaylist(this);
                if (!savedMusicList.isEmpty()) {
                    // 如果有保存的播放列表，更新音乐Fragment
                    Fragment musicFragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
                    if (musicFragment instanceof MusicFragment) {
                        ((MusicFragment) musicFragment).updateMusicList(savedMusicList);
                    }
                    Log.d(TAG, "已加载保存的音乐播放列表，共 " + savedMusicList.size() + " 项");
                    return;
                }
            }

            // 如果没有保存的播放列表或未启用保存功能，则加载默认音乐文件夹
            String defaultMusicFolderUri = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("default_music_folder_uri", null);
            if (defaultMusicFolderUri != null) {
                Uri musicFolderUri = Uri.parse(defaultMusicFolderUri);
                // 确保有权限访问该文件夹
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                try {
                    getContentResolver().takePersistableUriPermission(musicFolderUri, takeFlags);
                    loadMusicFromFolder(musicFolderUri);
                } catch (SecurityException e) {
                    Log.e(TAG, "无法获取音乐文件夹权限", e);
                    // 清除无效的文件夹设置
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .remove("default_music_folder_uri")
                            .apply();
                    // 清空音乐列表
                    Fragment musicFragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
                    if (musicFragment instanceof MusicFragment) {
                        ((MusicFragment) musicFragment).updateMusicList(new ArrayList<>());
                    }
                }
            } else {
                // 如果没有默认文件夹，清空音乐列表
                Fragment musicFragment = getSupportFragmentManager()
                        .findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
                if (musicFragment instanceof MusicFragment) {
                    ((MusicFragment) musicFragment).updateMusicList(new ArrayList<>());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "加载默认文件夹失败", e);
            // 发生异常时清空所有列表
            videoList.clear();
            updateVideoLists();
            Fragment musicFragment = getSupportFragmentManager()
                    .findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
            if (musicFragment instanceof MusicFragment) {
                ((MusicFragment) musicFragment).updateMusicList(new ArrayList<>());
            }
        }
    }

    public void loadMusicFromFolder(Uri folderUri) {
        try {
            Log.d(TAG, "开始扫描音乐文件夹: " + folderUri);
            
            // 先清空当前音乐列表
            Fragment musicFragment = getSupportFragmentManager().findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
            if (musicFragment instanceof MusicFragment) {
                ((MusicFragment) musicFragment).updateMusicList(new ArrayList<>());
            }
            
            // 创建独立线程处理文件扫描，避免阻塞UI线程
            new Thread(() -> {
                List<MusicAdapter.MusicItem> newMusic = new ArrayList<>();
                
                try {
                    DocumentFile folder = DocumentFile.fromTreeUri(this, folderUri);
                    
                    if (folder != null && folder.exists()) {
                        Log.d(TAG, "文件夹存在: " + folder.getName());
                        DocumentFile[] files = folder.listFiles();
                        Log.d(TAG, "找到文件数量: " + files.length);
                        
                        // 遍历查找音乐文件
                        for (DocumentFile file : files) {
                            try {
                                if (file != null && file.isFile() && file.getType() != null && file.getType().startsWith("audio/")) {
                                    String name = file.getName();
                                    Uri uri = file.getUri();
                                    if (name != null && uri != null) {
                                        Log.d(TAG, "找到音乐: " + name + ", URI: " + uri);
                                        // 获取音乐文件的元数据
                                        MusicMetadata metadata = getMusicMetadata(uri);
                                        newMusic.add(new MusicAdapter.MusicItem(
                                            uri,
                                            metadata.displayName,
                                            metadata.artist,
                                            metadata.album,
                                            metadata.duration
                                        ));
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "处理单个文件时出错", e);
                            }
                        }
                    } else {
                        Log.e(TAG, "文件夹不存在或无法访问");
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "无法访问音乐文件夹", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "扫描文件夹失败", e);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "扫描音乐文件夹失败", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // 对音乐列表进行排序（按名称字母顺序）
                sortMusicList(newMusic);
                
                Log.d(TAG, "找到音乐文件数量: " + newMusic.size());
                
                // 在UI线程更新界面
                runOnUiThread(() -> {
                    try {
                        if (musicFragment instanceof MusicFragment) {
                            if (newMusic.isEmpty()) {
                                ((MusicFragment) musicFragment).updateMusicList(new ArrayList<>());
                                Toast.makeText(MainActivity.this, R.string.no_music_found, Toast.LENGTH_SHORT).show();
                            } else {
                                ((MusicFragment) musicFragment).updateMusicList(newMusic);
                                Toast.makeText(MainActivity.this, 
                                        String.format("已加载 %d 个音乐文件", newMusic.size()), 
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        
                        // 确保标题栏显示应用名称，并禁用返回箭头
                        if (getSupportActionBar() != null && !isSettingsVisible) {
                            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                            getSupportActionBar().setTitle(R.string.app_name);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "更新UI失败", e);
                        Toast.makeText(MainActivity.this, "加载音乐列表失败", Toast.LENGTH_SHORT).show();
                    } finally {
                        // 确保刷新动画被关闭
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                            isRefreshing = false;
                        }
                    }
                });
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "加载文件夹中的音乐失败", e);
            Toast.makeText(this, "无法加载文件夹中的音乐", Toast.LENGTH_SHORT).show();
            // 确保刷新动画被关闭
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
                isRefreshing = false;
            }
            // 清空音乐列表
            Fragment musicFragment = getSupportFragmentManager().findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
            if (musicFragment instanceof MusicFragment) {
                ((MusicFragment) musicFragment).updateMusicList(new ArrayList<>());
            }
        }
    }

    private static class MusicMetadata {
        String displayName;
        String artist;
        String album;
        String duration;

        MusicMetadata(String displayName, String artist, String album, String duration) {
            this.displayName = displayName;
            this.artist = artist;
            this.album = album;
            this.duration = duration;
        }
    }

    private MusicMetadata getMusicMetadata(Uri uri) {
        String displayName = "未知音乐";
        String artist = "未知艺术家";
        String album = "未知专辑";
        String duration = "00:00";

        try {
            // 首先尝试从DocumentFile获取文件名
            DocumentFile file = DocumentFile.fromSingleUri(this, uri);
            if (file != null && file.getName() != null) {
                displayName = file.getName();
                // 去除文件扩展名
                int lastDot = displayName.lastIndexOf(".");
                if (lastDot > 0) {
                    displayName = displayName.substring(0, lastDot);
                }
            }
            
            // 使用MediaMetadataRetriever获取音频元数据
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, uri);
            
            // 获取艺术家信息
            String retrievedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            if (retrievedArtist != null && !retrievedArtist.isEmpty()) {
                artist = retrievedArtist;
            }
            
            // 获取专辑信息
            String retrievedAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            if (retrievedAlbum != null && !retrievedAlbum.isEmpty()) {
                album = retrievedAlbum;
            }
            
            // 获取持续时间
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                long durationMs = Long.parseLong(durationStr);
                duration = formatDuration(durationMs);
            }
            
            // 释放资源
            retriever.release();
            
            // 如果MediaMetadataRetriever无法获取到时长，尝试使用MediaStore
            if (duration.equals("00:00")) {
                try (Cursor cursor = getContentResolver().query(
                        uri,
                        new String[]{MediaStore.Audio.Media.DURATION},
                        null,
                        null,
                        null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                        long durationMs = cursor.getLong(durationColumn);
                        duration = formatDuration(durationMs);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "通过MediaStore获取音乐时长失败", e);
                }
            }
            
            Log.d(TAG, "获取到音乐元数据: 名称=" + displayName + ", 艺术家=" + artist + ", 专辑=" + album + ", 时长=" + duration);
            
        } catch (Exception e) {
            Log.e(TAG, "获取音乐元数据失败", e);
        }

        return new MusicMetadata(displayName, artist, album, duration);
    }

    private String formatDuration(long durationMs) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(durationMs),
                TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMs)));
    }

    private void sortMusicList(List<MusicAdapter.MusicItem> music) {
        if (music != null && !music.isEmpty()) {
            music.sort((m1, m2) -> {
                if (m1 == null || m1.getDisplayName() == null) return 1;
                if (m2 == null || m2.getDisplayName() == null) return -1;
                return m1.getDisplayName().toLowerCase().compareTo(m2.getDisplayName().toLowerCase());
            });
        }
    }

    public void updateBottomNavigation() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean showVideo = prefs.getBoolean("show_video_tab", true);
            boolean showMusic = prefs.getBoolean("show_music_tab", true);
            boolean showPlaylist = prefs.getBoolean("show_playlist_tab", true);

            // 获取底部导航菜单
            Menu menu = bottomNavigationView.getMenu();
            
            // 设置菜单项的可见性
            menu.findItem(R.id.nav_video).setVisible(showVideo);
            menu.findItem(R.id.nav_music).setVisible(showMusic);
            menu.findItem(R.id.nav_playlist).setVisible(showPlaylist);

            // 更新ViewPager适配器
            viewPagerAdapter.updateTabs(showVideo, showMusic, showPlaylist);

            // 如果当前选中的页面被隐藏，切换到第一个可见的页面
            int currentItem = viewPager.getCurrentItem();
            if (!viewPagerAdapter.isTabEnabled(currentItem)) {
                for (int i = 0; i < viewPagerAdapter.getItemCount(); i++) {
                    if (viewPagerAdapter.isTabEnabled(i)) {
                        viewPager.setCurrentItem(i, false);
                        bottomNavigationView.setSelectedItemId(getMenuItemIdForPosition(i));
                        break;
                    }
                }
            }
            
            // 检查音乐标签页是否可见，并且是否设置了音乐文件夹
            if (showMusic) {
                String defaultMusicFolderUri = prefs.getString("default_music_folder_uri", null);
                if (defaultMusicFolderUri == null) {
                    // 如果没有设置音乐文件夹，更新音乐Fragment显示提示信息
                    Fragment musicFragment = getSupportFragmentManager().findFragmentByTag("f" + ViewPagerAdapter.TAB_MUSIC);
                    if (musicFragment instanceof MusicFragment) {
                        ((MusicFragment) musicFragment).showEmptyView("请在设置中选择音乐文件夹");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "更新底部导航栏失败", e);
        }
    }

    public void reloadMusicList() {
        try {
            String defaultMusicFolderUri = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("default_music_folder_uri", null);
            if (defaultMusicFolderUri != null) {
                Uri musicFolderUri = Uri.parse(defaultMusicFolderUri);
                // 验证权限
                List<UriPermission> permissions = getContentResolver().getPersistedUriPermissions();
                boolean hasPermission = false;
                for (UriPermission permission : permissions) {
                    if (permission.getUri().equals(musicFolderUri) && 
                        permission.isReadPermission()) {
                        hasPermission = true;
                        break;
                    }
                }
                
                if (hasPermission) {
                    loadMusicFromFolder(musicFolderUri);
                } else {
                    // 如果没有权限，尝试重新获取
                    try {
                        int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(musicFolderUri, takeFlags);
                        loadMusicFromFolder(musicFolderUri);
                    } catch (SecurityException e) {
                        Log.e(TAG, "无法重新获取音乐文件夹权限", e);
                        // 清除无效的文件夹设置
                        PreferenceManager.getDefaultSharedPreferences(this)
                                .edit()
                                .remove("default_music_folder_uri")
                                .apply();
                        Toast.makeText(this, "无法访问音乐文件夹，请重新选择", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "重新加载音乐列表失败", e);
            Toast.makeText(this, "重新加载音乐列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (isSettingsVisible) {
            // 检查Fragment返回栈是否为空
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                // 如果有返回栈，先弹出栈顶Fragment
                getSupportFragmentManager().popBackStack();
            } else {
                // 如果返回栈为空，则关闭设置界面
                closeSettings();
            }
        } else {
            // 否则执行默认的返回操作
            super.onBackPressed();
        }
    }
}