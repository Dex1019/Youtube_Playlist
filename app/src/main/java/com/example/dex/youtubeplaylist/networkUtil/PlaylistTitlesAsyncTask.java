package com.example.dex.youtubeplaylist.networkUtil;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.example.dex.youtubeplaylist.ApiKey;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistListResponse;

import java.io.IOException;

public class PlaylistTitlesAsyncTask extends AsyncTask<String[], Void, PlaylistListResponse> {

    //see: https://developers.google.com/youtube/v3/docs/playlists/list
    private static final String YOUTUBE_PLAYLIST_PART = "snippet";
    private static final String YOUTUBE_PLAYLIST_FIELDS = "items(id,snippet(title))";

    private YouTube mYouTubeDataApi;

    public PlaylistTitlesAsyncTask(YouTube api) {
        mYouTubeDataApi = api;
    }

    @Override
    protected PlaylistListResponse doInBackground(String[]... params) {

        final String[] playlistIds = params[0];

        PlaylistListResponse playlistListResponse;

        try {
            playlistListResponse = mYouTubeDataApi.playlists()
                    .list(YOUTUBE_PLAYLIST_PART)
                    .setId(TextUtils.join(",", playlistIds))
                    .setFields(YOUTUBE_PLAYLIST_FIELDS)
                    .setKey(ApiKey.YOUTUBE_API_KEY)
                    .execute();

        } catch (IOException e) {
            e.printStackTrace();
            return null;

        }


        return playlistListResponse;
    }
}
