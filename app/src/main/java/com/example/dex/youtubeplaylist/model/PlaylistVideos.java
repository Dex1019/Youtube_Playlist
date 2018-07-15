package com.example.dex.youtubeplaylist.model;

import com.google.api.services.youtube.model.Video;

import java.util.ArrayList;

public class PlaylistVideos extends ArrayList<Video> {

    public final String playlistId;
    private String mNextPageToken;

    public PlaylistVideos(String playlistId) {
        this.playlistId = playlistId;
    }

    public String getNextPageToken() {
        return mNextPageToken;
    }

    public void setNextPageToken(String mNextPageToken) {
        this.mNextPageToken = mNextPageToken;
    }
}
