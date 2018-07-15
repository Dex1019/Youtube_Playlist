package com.example.dex.youtubeplaylist.networkUtil;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.example.dex.youtubeplaylist.ApiKey;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAsyncTask extends AsyncTask<String, Void, Pair<String, List<Video>>> {

    private static final String TAG = PlaylistAsyncTask.class.getSimpleName();
    private static final Long YOUTUBE_PLAYLIST_MAX_RESULTS = 10L;

    //see: https://developers.google.com/youtube/v3/docs/playlistItems/list
    private static final String YOUTUBE_PLAYLIST_PART = "snippet";
    private static final String YOUTUBE_PLAYLIST_FIELDS = "pageInfo,nextPageToken,items(id,snippet(resourceId/videoId))";
    //see: https://developers.google.com/youtube/v3/docs/videos/list
    private static final String YOUTUBE_VIDEOS_PART = "snippet,contentDetails,statistics"; // video resource properties that the response will include.
    private static final String YOUTUBE_VIDEOS_FIELDS = "items(id,snippet(title,description,thumbnails/high),contentDetails/duration,statistics)"; // selector specifying which fields to include in a partial response.

    private YouTube mYouTubeDataApi;

    public PlaylistAsyncTask(YouTube api) {
        mYouTubeDataApi = api;
    }


    //networking
    @Override
    protected Pair<String, List<Video>> doInBackground(String... params) {
        final String playlistId = params[0];
        final String nextPageToken;

        if (params.length == 2) {
            nextPageToken = params[1];
        } else {
            nextPageToken = null;
        }

        PlaylistItemListResponse playlistItemListResponse;
        try {

            playlistItemListResponse = mYouTubeDataApi.playlistItems()
                    .list(YOUTUBE_PLAYLIST_PART)
                    .setPlaylistId(playlistId)
                    .setPageToken(nextPageToken)
                    .setFields(YOUTUBE_PLAYLIST_FIELDS)
                    .setMaxResults(YOUTUBE_PLAYLIST_MAX_RESULTS)
                    .setKey(ApiKey.YOUTUBE_API_KEY)
                    .execute();


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (playlistItemListResponse == null) {
            Log.e(TAG, "Failed to get playlist");
            return null;
        }

        List<String> videoIds = new ArrayList<>();

        // pull out the video id's from the playlist page
        for (PlaylistItem item : playlistItemListResponse.getItems()) {
            videoIds.add(item.getSnippet().getResourceId().getVideoId());
        }

        // get details of the videos on this playlist page
        VideoListResponse videoListResponse = null;
        try {
            videoListResponse = mYouTubeDataApi.videos()
                    .list(YOUTUBE_VIDEOS_PART)
                    .setFields(YOUTUBE_VIDEOS_FIELDS)
                    .setKey(ApiKey.YOUTUBE_API_KEY)
                    .setId(TextUtils.join(",", videoIds)).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return new Pair(playlistItemListResponse.getNextPageToken(), videoListResponse.getItems());
    }
}
