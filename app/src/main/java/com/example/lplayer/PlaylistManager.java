package com.example.lplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 播放列表管理器，用于处理播放列表的持久化存储和加载
 */
public class PlaylistManager {
    private static final String TAG = "PlaylistManager";
    
    // SharedPreferences 键名
    private static final String KEY_VIDEO_PLAYLIST = "saved_video_playlist";
    private static final String KEY_MUSIC_PLAYLIST = "saved_music_playlist";
    
    // 保存视频播放列表
    public static void saveVideoPlaylist(Context context, List<VideoAdapter.VideoItem> videoList) {
        if (context == null || videoList == null) return;
        
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            
            // 检查是否启用了保存播放列表功能
            boolean saveEnabled = prefs.getBoolean("save_video_playlist", false);
            if (!saveEnabled) {
                // 如果未启用，则清除之前保存的播放列表
                prefs.edit().remove(KEY_VIDEO_PLAYLIST).apply();
                return;
            }
            
            // 将视频列表转换为可序列化的简单对象
            List<SerializableVideoItem> serializableList = new ArrayList<>();
            for (VideoAdapter.VideoItem item : videoList) {
                serializableList.add(new SerializableVideoItem(
                    item.getUri().toString(),
                    item.getDisplayName()
                ));
            }
            
            // 使用Gson将列表转换为JSON字符串
            Gson gson = new Gson();
            String json = gson.toJson(serializableList);
            
            // 保存到SharedPreferences
            prefs.edit().putString(KEY_VIDEO_PLAYLIST, json).apply();
            
            Log.d(TAG, "视频播放列表已保存，共 " + videoList.size() + " 项");
        } catch (Exception e) {
            Log.e(TAG, "保存视频播放列表失败: " + e.getMessage(), e);
        }
    }
    
    // 加载视频播放列表
    public static List<VideoAdapter.VideoItem> loadVideoPlaylist(Context context) {
        if (context == null) return new ArrayList<>();
        
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            
            // 检查是否启用了保存播放列表功能
            boolean saveEnabled = prefs.getBoolean("save_video_playlist", false);
            if (!saveEnabled) {
                return new ArrayList<>();
            }
            
            // 从SharedPreferences获取JSON字符串
            String json = prefs.getString(KEY_VIDEO_PLAYLIST, null);
            if (json == null) {
                return new ArrayList<>();
            }
            
            // 使用Gson将JSON字符串转换回对象列表
            Gson gson = new Gson();
            Type type = new TypeToken<List<SerializableVideoItem>>(){}.getType();
            List<SerializableVideoItem> serializableList = gson.fromJson(json, type);
            
            // 将可序列化对象转换回VideoItem
            List<VideoAdapter.VideoItem> videoList = new ArrayList<>();
            for (SerializableVideoItem item : serializableList) {
                try {
                    Uri uri = Uri.parse(item.uri);
                    videoList.add(new VideoAdapter.VideoItem(
                        uri,
                        item.displayName
                    ));
                } catch (Exception e) {
                    Log.e(TAG, "转换视频项失败: " + e.getMessage());
                    // 继续处理下一项
                }
            }
            
            Log.d(TAG, "视频播放列表已加载，共 " + videoList.size() + " 项");
            return videoList;
        } catch (Exception e) {
            Log.e(TAG, "加载视频播放列表失败: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // 保存音乐播放列表
    public static void saveMusicPlaylist(Context context, List<MusicAdapter.MusicItem> musicList) {
        if (context == null || musicList == null) return;
        
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            
            // 检查是否启用了保存播放列表功能
            boolean saveEnabled = prefs.getBoolean("save_music_playlist", false);
            if (!saveEnabled) {
                // 如果未启用，则清除之前保存的播放列表
                prefs.edit().remove(KEY_MUSIC_PLAYLIST).apply();
                return;
            }
            
            // 将音乐列表转换为可序列化的简单对象
            List<SerializableMusicItem> serializableList = new ArrayList<>();
            for (MusicAdapter.MusicItem item : musicList) {
                serializableList.add(new SerializableMusicItem(
                    item.getUri().toString(),
                    item.getDisplayName(),
                    item.getArtist(),
                    item.getAlbum(),
                    item.getDuration()
                ));
            }
            
            // 使用Gson将列表转换为JSON字符串
            Gson gson = new Gson();
            String json = gson.toJson(serializableList);
            
            // 保存到SharedPreferences
            prefs.edit().putString(KEY_MUSIC_PLAYLIST, json).apply();
            
            Log.d(TAG, "音乐播放列表已保存，共 " + musicList.size() + " 项");
        } catch (Exception e) {
            Log.e(TAG, "保存音乐播放列表失败: " + e.getMessage(), e);
        }
    }
    
    // 加载音乐播放列表
    public static List<MusicAdapter.MusicItem> loadMusicPlaylist(Context context) {
        if (context == null) return new ArrayList<>();
        
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            
            // 检查是否启用了保存播放列表功能
            boolean saveEnabled = prefs.getBoolean("save_music_playlist", false);
            if (!saveEnabled) {
                return new ArrayList<>();
            }
            
            // 从SharedPreferences获取JSON字符串
            String json = prefs.getString(KEY_MUSIC_PLAYLIST, null);
            if (json == null) {
                return new ArrayList<>();
            }
            
            // 使用Gson将JSON字符串转换回对象列表
            Gson gson = new Gson();
            Type type = new TypeToken<List<SerializableMusicItem>>(){}.getType();
            List<SerializableMusicItem> serializableList = gson.fromJson(json, type);
            
            // 将可序列化对象转换回MusicItem
            List<MusicAdapter.MusicItem> musicList = new ArrayList<>();
            for (SerializableMusicItem item : serializableList) {
                try {
                    Uri uri = Uri.parse(item.uri);
                    musicList.add(new MusicAdapter.MusicItem(
                        uri,
                        item.displayName,
                        item.artist,
                        item.album,
                        item.duration
                    ));
                } catch (Exception e) {
                    Log.e(TAG, "转换音乐项失败: " + e.getMessage());
                    // 继续处理下一项
                }
            }
            
            Log.d(TAG, "音乐播放列表已加载，共 " + musicList.size() + " 项");
            return musicList;
        } catch (Exception e) {
            Log.e(TAG, "加载音乐播放列表失败: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // 可序列化的视频项
    private static class SerializableVideoItem {
        String uri;
        String displayName;
        
        SerializableVideoItem(String uri, String displayName) {
            this.uri = uri;
            this.displayName = displayName;
        }
    }
    
    // 可序列化的音乐项
    private static class SerializableMusicItem {
        String uri;
        String displayName;
        String artist;
        String album;
        String duration;
        
        SerializableMusicItem(String uri, String displayName, String artist, String album, String duration) {
            this.uri = uri;
            this.displayName = displayName;
            this.artist = artist;
            this.album = album;
            this.duration = duration;
        }
    }
} 