package com.example.lplayer.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.lplayer.MainActivity;
import com.example.lplayer.R;

import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";
    private ListPreference playbackSpeedPreference;
    private Toolbar toolbar;
    private ActivityResultLauncher<Intent> videoFolderPickerLauncher;
    private ActivityResultLauncher<Intent> musicFolderPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化文件夹选择器
        videoFolderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == MainActivity.RESULT_OK && result.getData() != null) {
                    Uri folderUri = result.getData().getData();
                    if (folderUri != null) {
                        try {
                            // 获取持久化权限
                            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            requireContext().getContentResolver().takePersistableUriPermission(folderUri, takeFlags);
                            
                            // 验证权限是否成功获取
                            List<UriPermission> permissions = requireContext().getContentResolver().getPersistedUriPermissions();
                            boolean hasPermission = false;
                            for (UriPermission permission : permissions) {
                                if (permission.getUri().equals(folderUri) && 
                                    permission.isReadPermission()) {
                                    hasPermission = true;
                                    break;
                                }
                            }
                            
                            if (hasPermission) {
                                // 保存视频文件夹URI
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                                editor.putString("default_video_folder_uri", folderUri.toString());
                                editor.apply();
                                
                                // 更新设置项摘要
                                Preference videoFolderPref = findPreference("default_video_folder");
                                if (videoFolderPref != null) {
                                    videoFolderPref.setSummary("已选择: " + getFolderNameFromUri(folderUri));
                                }
                            } else {
                                Toast.makeText(requireContext(), "无法获取文件夹访问权限，请重试", Toast.LENGTH_SHORT).show();
                            }
                        } catch (SecurityException e) {
                            Log.e(TAG, "获取文件夹权限失败", e);
                            Toast.makeText(requireContext(), "无法获取文件夹访问权限: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

        musicFolderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == MainActivity.RESULT_OK && result.getData() != null) {
                    Uri folderUri = result.getData().getData();
                    if (folderUri != null) {
                        try {
                            // 获取持久化权限
                            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            requireContext().getContentResolver().takePersistableUriPermission(folderUri, takeFlags);
                            
                            // 验证权限是否成功获取
                            List<UriPermission> permissions = requireContext().getContentResolver().getPersistedUriPermissions();
                            boolean hasPermission = false;
                            for (UriPermission permission : permissions) {
                                if (permission.getUri().equals(folderUri) && 
                                    permission.isReadPermission()) {
                                    hasPermission = true;
                                    break;
                                }
                            }
                            
                            if (hasPermission) {
                                // 保存音乐文件夹URI
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
                                editor.putString("default_music_folder_uri", folderUri.toString());
                                editor.apply();
                                
                                // 更新设置项摘要
                                Preference musicFolderPref = findPreference("default_music_folder");
                                if (musicFolderPref != null) {
                                    musicFolderPref.setSummary("已选择: " + getFolderNameFromUri(folderUri));
                                }
                                
                                // 通知 MainActivity 重新加载音乐列表
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).reloadMusicList();
                                }
                            } else {
                                Toast.makeText(requireContext(), "无法获取文件夹访问权限，请重试", Toast.LENGTH_SHORT).show();
                            }
                        } catch (SecurityException e) {
                            Log.e(TAG, "获取文件夹权限失败", e);
                            Toast.makeText(requireContext(), "无法获取文件夹访问权限: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
        
        // 获取播放速度设置项
        playbackSpeedPreference = findPreference("default_playback_speed");
        if (playbackSpeedPreference != null) {
            // 初始化显示当前值
            updatePlaybackSpeedSummary(playbackSpeedPreference.getValue());
        }

        // 设置默认视频文件夹点击事件
        Preference videoFolderPref = findPreference("default_video_folder");
        if (videoFolderPref != null) {
            // 显示当前选择的文件夹
            String videoFolderUri = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("default_video_folder_uri", null);
            if (videoFolderUri != null) {
                videoFolderPref.setSummary("已选择: " + getFolderNameFromUri(Uri.parse(videoFolderUri)));
            }
            
            videoFolderPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                videoFolderPickerLauncher.launch(intent);
                return true;
            });
        }

        // 设置默认音乐文件夹点击事件
        Preference musicFolderPref = findPreference("default_music_folder");
        if (musicFolderPref != null) {
            // 显示当前选择的文件夹
            String musicFolderUri = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("default_music_folder_uri", null);
            if (musicFolderUri != null) {
                musicFolderPref.setSummary("已选择: " + getFolderNameFromUri(Uri.parse(musicFolderUri)));
            }
            
            musicFolderPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                musicFolderPickerLauncher.launch(intent);
                return true;
            });
        }

        // 设置关于应用点击事件
        Preference aboutApp = findPreference("about_app");
        if (aboutApp != null) {
            aboutApp.setOnPreferenceClickListener(preference -> {
                showAboutFragment();
                return true;
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
        
        // 恢复时更新显示
        if (playbackSpeedPreference != null) {
            updatePlaybackSpeedSummary(playbackSpeedPreference.getValue());
        }
        setupToolbar();
    }

    private void setupToolbar() {
        if (toolbar == null) {
            toolbar = requireActivity().findViewById(R.id.toolbar);
        }
        if (toolbar != null) {
            toolbar.setTitle("设置");
            toolbar.setNavigationIcon(R.drawable.ic_back);
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).closeSettings();
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (toolbar != null) {
            toolbar.setTitle(R.string.app_name);
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
            toolbar = null;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "show_video_tab":
            case "show_music_tab":
            case "show_playlist_tab":
                // 通知MainActivity更新底部导航栏
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).updateBottomNavigation();
                }
                break;
            case "default_playback_speed":
                String speed = sharedPreferences.getString(key, "1.0");
                updatePlaybackSpeedSummary(speed);
                break;
            case "default_video_folder_uri":
                // ... existing code ...
                break;
            case "default_music_folder_uri":
                // 当设置了音乐文件夹时，通知MainActivity重新加载音乐列表
                String musicFolderUri = sharedPreferences.getString(key, null);
                if (musicFolderUri != null && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).reloadMusicList();
                }
                break;
        }
    }

    private void updatePlaybackSpeedSummary(String speedValue) {
        if (playbackSpeedPreference != null) {
            // 获取对应的显示文本
            String[] entries = getResources().getStringArray(R.array.playback_speed_entries);
            String[] values = getResources().getStringArray(R.array.playback_speed_values);
            
            // 查找当前值对应的显示文本
            String displayText = "1.0x"; // 默认值
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(speedValue)) {
                    displayText = entries[i];
                    break;
                }
            }
            
            // 更新摘要显示
            playbackSpeedPreference.setSummary("当前速度: " + displayText);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if (key != null) {
            // 移除display_mode相关代码
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void showAboutFragment() {
        AboutFragment fragment = new AboutFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // ... existing code ...
    }

    private String getFolderNameFromUri(Uri uri) {
        try {
            String lastPath = uri.getLastPathSegment();
            if (lastPath != null) {
                return lastPath;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "未知文件夹";
    }
} 