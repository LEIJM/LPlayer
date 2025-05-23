package com.example.lplayer;

import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {
    
    private static final String TAG = "MusicAdapter";
    private List<MusicItem> musicList = new ArrayList<>();
    private OnMusicClickListener listener;
    private int currentPlayingPosition = -1;
    
    public interface OnMusicClickListener {
        void onMusicClick(MusicItem musicItem);
    }
    
    public void setMusicList(List<MusicItem> musicList) {
        if (musicList == null) {
            this.musicList = new ArrayList<>();
        } else {
            this.musicList = new ArrayList<>(musicList); // 创建副本以避免外部修改
        }
        // 不重置当前播放位置，避免UI跳跃
        // currentPlayingPosition = -1; // 重置当前播放位置
        notifyDataSetChanged();
        Log.d(TAG, "设置了" + this.musicList.size() + "首音乐到适配器");
    }
    
    public List<MusicItem> getMusicList() {
        return new ArrayList<>(musicList); // 返回副本以避免外部修改
    }
    
    public void setCurrentPlayingPosition(int position) {
        if (position >= -1 && position < musicList.size()) {
            int oldPosition = currentPlayingPosition;
            currentPlayingPosition = position;
            
            // 刷新旧位置和新位置的项
            if (oldPosition != -1) {
                notifyItemChanged(oldPosition);
            }
            if (currentPlayingPosition != -1) {
                notifyItemChanged(currentPlayingPosition);
            }
            
            Log.d(TAG, "设置当前播放位置: " + position);
        }
    }
    
    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }
    
    public int getPositionForItem(MusicItem item) {
        if (item != null) {
            for (int i = 0; i < musicList.size(); i++) {
                MusicItem current = musicList.get(i);
                if (current != null && current.getUri() != null && 
                    current.getUri().equals(item.getUri())) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    public void setOnMusicClickListener(OnMusicClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.music_item, parent, false);
        return new MusicViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        try {
            MusicItem musicItem = musicList.get(position);
            if (musicItem != null) {
                String displayName = musicItem.getDisplayName();
                String artist = musicItem.getArtist();
                String duration = musicItem.getDuration();
                
                holder.musicTitle.setText(displayName != null ? displayName : "未知音乐");
                holder.musicArtist.setText(artist != null ? artist : "未知艺术家");
                holder.musicDuration.setText(duration != null ? duration : "");
                
                // 设置当前播放项的高亮效果
                if (position == currentPlayingPosition) {
                    holder.itemView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.playing_item_background));
                    holder.musicTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorAccent));
                    holder.musicArtist.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextPrimary));
                    holder.musicDuration.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorAccent));
                } else {
                    holder.itemView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.music_item_background));
                    holder.musicTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextPrimary));
                    holder.musicArtist.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextSecondary));
                    holder.musicDuration.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextSecondary));
                }
                
                holder.itemView.setOnClickListener(v -> {
                    try {
                        if (listener != null) {
                            listener.onMusicClick(musicItem);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "音乐项点击处理错误", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "绑定音乐项时出错", e);
        }
    }
    
    @Override
    public int getItemCount() {
        return musicList.size();
    }
    
    static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView musicTitle;
        TextView musicArtist;
        TextView musicDuration;
        
        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            musicTitle = itemView.findViewById(R.id.music_title);
            musicArtist = itemView.findViewById(R.id.music_artist);
            musicDuration = itemView.findViewById(R.id.music_duration);
        }
    }
    
    public static class MusicItem {
        private final Uri uri;
        private final String displayName;
        private final String artist;
        private final String album;
        private final String duration;
        
        public MusicItem(Uri uri, String displayName, String artist, String album, String duration) {
            this.uri = uri;
            this.displayName = displayName;
            this.artist = artist;
            this.album = album;
            this.duration = duration;
        }
        
        public Uri getUri() {
            return uri;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getArtist() {
            return artist;
        }
        
        public String getAlbum() {
            return album;
        }
        
        public String getDuration() {
            return duration;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MusicItem musicItem = (MusicItem) obj;
            return uri != null && uri.equals(musicItem.uri);
        }
        
        @Override
        public int hashCode() {
            return uri != null ? uri.hashCode() : 0;
        }
    }
}