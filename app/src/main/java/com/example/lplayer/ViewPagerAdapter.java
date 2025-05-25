package com.example.lplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.lplayer.fragments.MusicFragment;
import com.example.lplayer.fragments.PlaylistFragment;
import com.example.lplayer.fragments.VideoFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public static final int TAB_VIDEO = 0;
    public static final int TAB_MUSIC = 1;
    public static final int TAB_PLAYLIST = 2;

    private boolean[] tabEnabled = {true, true, true}; // 默认所有标签页都启用
    private int[] enabledTabs; // 实际启用的标签页索引

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        updateEnabledTabs();
    }

    public void updateTabs(boolean showVideo, boolean showMusic, boolean showPlaylist) {
        tabEnabled[TAB_VIDEO] = showVideo;
        tabEnabled[TAB_MUSIC] = showMusic;
        tabEnabled[TAB_PLAYLIST] = showPlaylist;
        updateEnabledTabs();
        notifyDataSetChanged();
    }

    private void updateEnabledTabs() {
        // 计算启用的标签页数量
        int count = 0;
        for (boolean enabled : tabEnabled) {
            if (enabled) count++;
        }

        // 创建启用的标签页数组
        enabledTabs = new int[count];
        int index = 0;
        for (int i = 0; i < tabEnabled.length; i++) {
            if (tabEnabled[i]) {
                enabledTabs[index++] = i;
            }
        }
    }

    public int[] getEnabledTabs() {
        return enabledTabs;
    }

    public boolean isTabEnabled(int position) {
        return position >= 0 && position < enabledTabs.length;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (!isTabEnabled(position)) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }

        int tabType = enabledTabs[position];
        switch (tabType) {
            case TAB_VIDEO:
                return VideoFragment.newInstance();
            case TAB_MUSIC:
                return MusicFragment.newInstance();
            case TAB_PLAYLIST:
                return PlaylistFragment.newInstance();
            default:
                throw new IllegalArgumentException("Invalid tab type: " + tabType);
        }
    }

    @Override
    public int getItemCount() {
        return enabledTabs.length;
    }

    @Override
    public long getItemId(int position) {
        // 使用启用的标签页类型作为ID，确保Fragment正确重建
        return enabledTabs[position];
    }

    @Override
    public boolean containsItem(long itemId) {
        // 检查itemId是否在启用的标签页中
        for (int tab : enabledTabs) {
            if (tab == itemId) {
                return true;
            }
        }
        return false;
    }
} 