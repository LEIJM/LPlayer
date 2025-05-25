package com.example.lplayer.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.lplayer.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);
        
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
                // TODO: 实现关于应用界面
                Toast.makeText(getContext(), "关于应用功能开发中", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // 设置隐私政策点击事件
        Preference privacyPolicy = findPreference("privacy_policy");
        if (privacyPolicy != null) {
            privacyPolicy.setOnPreferenceClickListener(preference -> {
                // TODO: 替换为实际的隐私政策URL
                String privacyPolicyUrl = "https://example.com/privacy-policy";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
                startActivity(intent);
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
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
} 