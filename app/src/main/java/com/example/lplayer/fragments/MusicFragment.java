package com.example.lplayer.fragments;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lplayer.MusicAdapter;
import com.example.lplayer.MusicPlayerActivity;
import com.example.lplayer.R;
import com.example.lplayer.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicFragment extends Fragment {

    private static final String TAG = "MusicFragment";
    
    private RecyclerView recyclerView;
    private TextView emptyView;
    public MusicAdapter musicAdapter;
    private List<MusicAdapter.MusicItem> musicList = new ArrayList<>();
    private MusicFragmentListener listener;
    
    public interface MusicFragmentListener {
        void onMusicItemClicked(MusicAdapter.MusicItem musicItem);
    }
    
    public MusicFragment() {
        // Required empty public constructor
    }

    public static MusicFragment newInstance() {
        return new MusicFragment();
    }
    
    public void setMusicFragmentListener(MusicFragmentListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化视图
        recyclerView = view.findViewById(R.id.music_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        
        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicAdapter = new MusicAdapter();
        recyclerView.setAdapter(musicAdapter);
        
        // 设置点击监听器
        musicAdapter.setOnMusicClickListener(musicItem -> {
            if (listener != null) {
                listener.onMusicItemClicked(musicItem);
            } else {
                // 如果没有设置监听器，直接启动音乐播放器
                playMusic(musicItem);
            }
        });
        
        // 检查并加载音乐
        checkAndLoadMusic();
    }
    
    private void checkAndLoadMusic() {
        try {
            // 检查是否有默认文件夹设置
            String defaultMusicFolderUri = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("default_music_folder_uri", null);
            
            if (defaultMusicFolderUri != null) {
                // 如果有默认文件夹，通知 MainActivity 加载
                Log.d(TAG, "检测到默认音乐文件夹设置，通知 MainActivity 加载");
                showEmptyView("正在加载音乐文件夹...");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadMusicFromFolder(Uri.parse(defaultMusicFolderUri));
                }
            } else {
                // 如果没有默认文件夹，显示提示信息
                showEmptyView("请在设置中选择音乐文件夹");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查音乐文件夹时出错: " + e.getMessage(), e);
            Toast.makeText(getContext(), "无法访问音乐文件夹", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String formatDuration(long durationMs) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(durationMs),
                TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMs)));
    }
    
    private void updateUI() {
        if (getContext() == null) return;
        
        if (musicList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(R.string.no_music_found);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            musicAdapter.setMusicList(musicList);
        }
    }
    
    public List<MusicAdapter.MusicItem> getMusicList() {
        return musicList;
    }
    
    private void playMusic(MusicAdapter.MusicItem musicItem) {
        try {
            Intent intent = new Intent(getActivity(), MusicPlayerActivity.class);
            intent.putExtra("music_uri", musicItem.getUri().toString());
            intent.putExtra("music_title", musicItem.getDisplayName());
            intent.putExtra("music_artist", musicItem.getArtist());
            
            // 构建播放列表数据
            ArrayList<String> uriStrings = new ArrayList<>();
            ArrayList<String> titles = new ArrayList<>();
            ArrayList<String> artists = new ArrayList<>();
            ArrayList<String> albums = new ArrayList<>();
            ArrayList<String> durations = new ArrayList<>();
            
            for (MusicAdapter.MusicItem item : musicList) {
                uriStrings.add(item.getUri().toString());
                titles.add(item.getDisplayName());
                artists.add(item.getArtist());
                albums.add(item.getAlbum());
                durations.add(item.getDuration());
            }
            
            intent.putStringArrayListExtra("playlist", uriStrings);
            intent.putStringArrayListExtra("playlist_titles", titles);
            intent.putStringArrayListExtra("playlist_artists", artists);
            intent.putStringArrayListExtra("playlist_albums", albums);
            intent.putStringArrayListExtra("playlist_durations", durations);
            
            // 设置当前播放位置
            int currentPosition = musicAdapter.getPositionForItem(musicItem);
            intent.putExtra("current_position", currentPosition);
            
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "启动音乐播放器失败: " + e.getMessage(), e);
            Toast.makeText(getContext(), "无法播放音乐: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 检查是否有默认文件夹设置
        String defaultMusicFolderUri = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("default_music_folder_uri", null);
        
        if (defaultMusicFolderUri != null) {
            // 如果有默认文件夹，重新加载
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadMusicFromFolder(Uri.parse(defaultMusicFolderUri));
            }
        } else {
            // 如果没有默认文件夹，显示提示信息
            showEmptyView("请在设置中选择音乐文件夹");
        }
    }

    public void updateMusicList(List<MusicAdapter.MusicItem> newMusicList) {
        if (getContext() == null) return;
        
        musicList.clear();
        if (newMusicList != null) {
            musicList.addAll(newMusicList);
        }
        updateUI();
    }

    public void showEmptyView(String message) {
        if (getContext() == null) return;
        
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText(message);
    }
}