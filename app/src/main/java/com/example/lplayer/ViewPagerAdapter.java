package com.example.lplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.lplayer.fragments.MusicFragment;
import com.example.lplayer.fragments.PlaylistFragment;
import com.example.lplayer.fragments.VideoFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private static final int NUM_TABS = 3;
    public static final int TAB_VIDEO = 0;
    public static final int TAB_MUSIC = 1;
    public static final int TAB_PLAYLIST = 2;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_VIDEO:
                return VideoFragment.newInstance();
            case TAB_MUSIC:
                return MusicFragment.newInstance();
            case TAB_PLAYLIST:
                return PlaylistFragment.newInstance();
            default:
                return VideoFragment.newInstance();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_TABS;
    }
} 