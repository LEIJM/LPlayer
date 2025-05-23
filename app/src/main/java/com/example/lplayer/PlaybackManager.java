package com.example.lplayer;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class PlaybackManager {
    private static PlaybackManager instance;
    private List<VideoAdapter.VideoItem> videoList = new ArrayList<>();
    private int currentPlayingPosition = -1;

    private PlaybackManager() {}

    public static synchronized PlaybackManager getInstance() {
        if (instance == null) {
            instance = new PlaybackManager();
        }
        return instance;
    }

    public List<VideoAdapter.VideoItem> getVideoList() {
        return videoList;
    }

    public void setVideoList(List<VideoAdapter.VideoItem> list) {
        videoList.clear();
        if (list != null) {
            videoList.addAll(list);
        }
    }

    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }

    public void setCurrentPlayingPosition(int pos) {
        this.currentPlayingPosition = pos;
    }

    public VideoAdapter.VideoItem getCurrentVideoItem() {
        if (currentPlayingPosition >= 0 && currentPlayingPosition < videoList.size()) {
            return videoList.get(currentPlayingPosition);
        }
        return null;
    }
}