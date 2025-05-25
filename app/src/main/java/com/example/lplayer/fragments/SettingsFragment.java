package com.example.lplayer.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.lplayer.MainActivity;
import com.example.lplayer.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ListPreference playbackSpeedPreference;
    private Toolbar toolbar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
        
        // 获取播放速度设置项
        playbackSpeedPreference = findPreference("default_playback_speed");
        if (playbackSpeedPreference != null) {
            // 初始化显示当前值
            updatePlaybackSpeedSummary(playbackSpeedPreference.getValue());
        }
        
        // 设置缓存位置点击事件
        Preference cacheLocation = findPreference("cache_location");
        if (cacheLocation != null) {
            cacheLocation.setOnPreferenceClickListener(preference -> {
                // TODO: 实现缓存位置选择逻辑
                Toast.makeText(getContext(), "缓存位置选择功能开发中", Toast.LENGTH_SHORT).show();
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
            case "default_playback_speed":
                String speed = sharedPreferences.getString(key, "1.0");
                updatePlaybackSpeedSummary(speed);
                break;
            case "cache_size":
                int cacheSize = sharedPreferences.getInt(key, 2);
                // TODO: 实现缓存大小更新逻辑
                break;
            case "list_display_mode":
                String displayMode = sharedPreferences.getString(key, "grid");
                // TODO: 实现列表显示模式更新逻辑
                break;
            case "thumbnail_quality":
                String thumbnailQuality = sharedPreferences.getString(key, "medium");
                // TODO: 实现缩略图质量更新逻辑
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
            switch (key) {
                case "display_mode":
                    // 处理显示模式设置
                    String displayMode = preference.getSharedPreferences()
                            .getString("display_mode", "grid");
                    sendDisplayModeBroadcast(displayMode);
                    break;
            }
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

    private void sendDisplayModeBroadcast(String displayMode) {
        Intent intent = new Intent("com.example.lplayer.DISPLAY_MODE_CHANGED");
        intent.putExtra("display_mode", displayMode);
        requireContext().sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // ... existing code ...
    }
} 