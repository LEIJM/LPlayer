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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lplayer.MusicAdapter;
import com.example.lplayer.MusicPlayerActivity;
import com.example.lplayer.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicFragment extends Fragment {

    private static final String TAG = "MusicFragment";
    private static final int PERMISSION_REQUEST_CODE = 101;
    
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
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
        
        // 检查权限并加载音乐
        checkPermissionAndLoadMusic();
    }
    
    private void checkPermissionAndLoadMusic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13及以上使用READ_MEDIA_AUDIO权限
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                            PERMISSION_REQUEST_CODE);
                } else {
                    loadMusicFromStorage();
                }
            } else {
                // Android 12及以下使用READ_EXTERNAL_STORAGE权限
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                } else {
                    loadMusicFromStorage();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "检查权限时出错: " + e.getMessage(), e);
            Toast.makeText(getContext(), "无法访问音乐文件", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicFromStorage();
            } else {
                Toast.makeText(getContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    
    private void loadMusicFromStorage() {
        try {
            musicList.clear();
            
            // 查询音频文件
            Uri collection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
            
            String[] projection = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION
            };
            
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            
            try (Cursor cursor = requireContext().getContentResolver().query(
                    collection,
                    projection,
                    selection,
                    null,
                    MediaStore.Audio.Media.DATE_ADDED + " DESC")) {
                
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                    int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                    int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    
                    Log.d(TAG, "开始加载音乐文件，共找到 " + cursor.getCount() + " 个文件");
                    
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        String name = cursor.getString(nameColumn);
                        String artist = cursor.getString(artistColumn);
                        String album = cursor.getString(albumColumn);
                        long duration = cursor.getLong(durationColumn);
                        
                        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                        
                        // 格式化时长
                        String durationFormatted = formatDuration(duration);
                        
                        // 创建音乐项并添加到列表
                        musicList.add(new MusicAdapter.MusicItem(contentUri, name, artist, album, durationFormatted));
                    }
                }
            }
            
            // 更新UI
            updateUI();
            
        } catch (Exception e) {
            Log.e(TAG, "加载音乐文件时出错: " + e.getMessage(), e);
            Toast.makeText(getContext(), "加载音乐文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        // 每次恢复时刷新音乐列表
        if (ContextCompat.checkSelfPermission(requireContext(), 
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? 
                        Manifest.permission.READ_MEDIA_AUDIO : 
                        Manifest.permission.READ_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED) {
            loadMusicFromStorage();
        }
    }

    public void updateMusicList(List<MusicAdapter.MusicItem> newMusicList) {
        if (newMusicList != null) {
            musicList.clear();
            musicList.addAll(newMusicList);
            updateUI();
        }
    }
}