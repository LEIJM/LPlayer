package com.example.lplayer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lplayer.R;
import com.example.lplayer.VideoAdapter;

public class VideoFragment extends Fragment {

    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private TextView emptyView;

    public VideoFragment() {
        // Required empty public constructor
    }

    public static VideoFragment newInstance() {
        return new VideoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图
        recyclerView = view.findViewById(R.id.video_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);

        // 设置适配器
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        videoAdapter = new VideoAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(videoAdapter);
        
        videoAdapter.setOnVideoClickListener(videoItem -> {
            if (getActivity() instanceof VideoFragmentListener) {
                ((VideoFragmentListener) getActivity()).onVideoItemClicked(videoItem);
            }
        });
    }

    public void setVideos(java.util.List<VideoAdapter.VideoItem> videos) {
        if (videoAdapter != null) {
            videoAdapter.setVideoList(videos);
            
            // 更新空视图的可见性
            if (videos.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    public void setCurrentPlayingPosition(int position) {
        if (videoAdapter != null) {
            videoAdapter.setCurrentPlayingPosition(position);
        }
    }

    public interface VideoFragmentListener {
        void onVideoItemClicked(VideoAdapter.VideoItem videoItem);
    }
} 