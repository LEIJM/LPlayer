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

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    
    private static final String TAG = "VideoAdapter";
    private List<VideoItem> videoList = new ArrayList<>();
    private OnVideoClickListener listener;
    private int currentPlayingPosition = -1;
    
    public interface OnVideoClickListener {
        void onVideoClick(VideoItem videoItem);
    }
    
    public void setVideoList(List<VideoItem> videoList) {
        if (videoList == null) {
            this.videoList = new ArrayList<>();
        } else {
            this.videoList = new ArrayList<>(videoList); // 创建副本以避免外部修改
        }
        // 不重置当前播放位置，避免UI跳跃
        // currentPlayingPosition = -1; // 重置当前播放位置
        notifyDataSetChanged();
        Log.d(TAG, "设置了" + this.videoList.size() + "个视频到适配器");
    }
    
    public List<VideoItem> getVideoList() {
        return new ArrayList<>(videoList); // 返回副本以避免外部修改
    }
    
    public void setCurrentPlayingPosition(int position) {
        if (position >= -1 && position < videoList.size()) {
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
    
    public int getPositionForItem(VideoItem item) {
        if (item != null) {
            for (int i = 0; i < videoList.size(); i++) {
                VideoItem current = videoList.get(i);
                if (current != null && current.getUri() != null && 
                    current.getUri().equals(item.getUri())) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        try {
            VideoItem videoItem = videoList.get(position);
            if (videoItem != null) {
                String displayName = videoItem.getDisplayName();
                holder.videoTitle.setText(displayName != null ? displayName : "未知视频");
                
                // 设置当前播放项的高亮效果
                if (position == currentPlayingPosition) {
                    holder.itemView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.playing_item_background));
                    holder.videoTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorAccent));
                } else {
                    holder.itemView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.music_item_background));
                    holder.videoTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextPrimary));
                }
                
                holder.itemView.setOnClickListener(v -> {
                    try {
                        if (listener != null) {
                            listener.onVideoClick(videoItem);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "视频项点击处理错误", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "绑定视频项时出错", e);
        }
    }
    
    @Override
    public int getItemCount() {
        return videoList.size();
    }
    
    static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView videoTitle;
        
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoTitle = itemView.findViewById(R.id.video_title);
        }
    }
    
    public static class VideoItem {
        private final Uri uri;
        private final String displayName;
        
        public VideoItem(Uri uri, String displayName) {
            this.uri = uri;
            this.displayName = displayName;
        }
        
        public Uri getUri() {
            return uri;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            VideoItem videoItem = (VideoItem) obj;
            return uri != null && uri.equals(videoItem.uri);
        }
        
        @Override
        public int hashCode() {
            return uri != null ? uri.hashCode() : 0;
        }
    }
}