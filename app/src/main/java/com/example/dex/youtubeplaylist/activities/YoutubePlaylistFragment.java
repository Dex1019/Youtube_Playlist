package com.example.dex.youtubeplaylist.activities;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.example.dex.youtubeplaylist.R;
import com.example.dex.youtubeplaylist.adapter.PlaylistCardviewAdapter;
import com.example.dex.youtubeplaylist.model.PlaylistVideos;
import com.example.dex.youtubeplaylist.networkUtil.PlaylistAsyncTask;
import com.example.dex.youtubeplaylist.networkUtil.PlaylistTitlesAsyncTask;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.api.services.youtube.model.Video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YoutubePlaylistFragment extends Fragment {

    // the fragment initialization parameter
    private static final String ARG_YOUTUBE_PLAYLIST_IDS = "YOUTUBE_PLAYLIST_IDS";
    private static final int SPINNER_ITEM_LAYOUT_ID = android.R.layout.simple_spinner_item;
    private static final int SPINNER_ITEM_DROPDOWN_LAYOUT_ID = android.R.layout.simple_spinner_dropdown_item;

    private String[] mPlaylistIds;
    private ArrayList<String> mPlaylistTitles;
    private RecyclerView mRecyclerView;
    private PlaylistVideos mPlaylistVideos;
    private RecyclerView.LayoutManager mLayoutManager;
    private Spinner mPlaylistSpinner;
    private PlaylistCardviewAdapter mPlaylistCardAdapter;
    private YouTube mYouTubeDataApi;
    private ProgressDialog mProgressDialog;
    private ProgressBar progressBar;
//    private Button mLoadMoreContent;


    public YoutubePlaylistFragment() {
        // Required empty public constructor
    }

    public static YoutubePlaylistFragment newInstance(YouTube youTubeDataApi, String[] playlistIds) {
        YoutubePlaylistFragment fragment = new YoutubePlaylistFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_YOUTUBE_PLAYLIST_IDS, playlistIds);
        fragment.setArguments(args);
        fragment.setYouTubeDataApi(youTubeDataApi);
        return fragment;
    }

    public void setYouTubeDataApi(YouTube api) {
        mYouTubeDataApi = api;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (getArguments() != null) {
            mPlaylistIds = getArguments().getStringArray(ARG_YOUTUBE_PLAYLIST_IDS);
        }


        // start fetching the playlist titles
        new PlaylistTitlesAsyncTask(mYouTubeDataApi) {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(PlaylistListResponse playlistListResponse) {
                // if we didn't receive a response for the playlist titles, then there's nothing to update

                if (playlistListResponse == null) {
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                mPlaylistTitles = new ArrayList<>();
                for (com.google.api.services.youtube.model.Playlist playlist : playlistListResponse.getItems()) {
                    mPlaylistTitles.add(playlist.getSnippet().getTitle());
                    progressBar.setVisibility(View.GONE);
                }

                // update the spinner adapter with the titles of the playlists
                ArrayAdapter<List<String>> spinnerAdapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, mPlaylistTitles);
                spinnerAdapter.setDropDownViewResource(SPINNER_ITEM_DROPDOWN_LAYOUT_ID);
                mPlaylistSpinner.setAdapter(spinnerAdapter);

            }
        }.execute(mPlaylistIds);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_youtube_playlist, container, false);

        mRecyclerView = rootView.findViewById(R.id.youtube_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mProgressDialog = new ProgressDialog(getContext());
        progressBar = rootView.findViewById(R.id.progressBar);

        mPlaylistSpinner = rootView.findViewById(R.id.youtube_playlist_spinner);
//        mLoadMoreContent = rootView.findViewById(R.id.btn_load_more);

        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // if we have a playlist in our retained fragment, use it to populate the UI
        if (mPlaylistVideos != null) {
            // reload the UI with the existing playlist.  No need to fetch it again
            reloadUi(mPlaylistVideos, false);
        } else {
            // otherwise create an empty playlist using the first item in the playlist id's array
            mPlaylistVideos = new PlaylistVideos(mPlaylistIds[0]);
            // and reload the UI with the selected playlist and kick off fetching the playlist content
            reloadUi(mPlaylistVideos, true);
        }

        ArrayAdapter<List<String>> spinnerAdapter;
        // if we don't have the playlist titles yet
        if (mPlaylistTitles == null || mPlaylistTitles.isEmpty()) {
            // initialize the spinner with the playlist ID's so that there's something in the UI until the PlaylistTitlesAsyncTask finishes
            spinnerAdapter = new ArrayAdapter(getContext(), SPINNER_ITEM_LAYOUT_ID, Arrays.asList(mPlaylistIds));
        } else {
            // otherwise use the playlist titles for the spinner
            spinnerAdapter = new ArrayAdapter(getContext(), SPINNER_ITEM_LAYOUT_ID, mPlaylistTitles);
        }

        spinnerAdapter.setDropDownViewResource(SPINNER_ITEM_DROPDOWN_LAYOUT_ID);
        mPlaylistSpinner.setAdapter(spinnerAdapter);

        // set up the onItemSelectedListener for the spinner
        mPlaylistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // reload the UI with the playlist video list of the selected playlist
                mPlaylistVideos = new PlaylistVideos(mPlaylistIds[position]);
                reloadUi(mPlaylistVideos, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }


    private void reloadUi(final PlaylistVideos playlistVideos, boolean fetchPlaylist) {
        // initialize the cards adapter
        initCardAdapter(playlistVideos);

        if (fetchPlaylist) {
            // start fetching the selected playlistVideos contents
            new PlaylistAsyncTask(mYouTubeDataApi) {
                @Override
                public void onPostExecute(Pair<String, List<Video>> result) {
                    handleGetPlaylistResult(playlistVideos, result);
                }
            }.execute(playlistVideos.playlistId, playlistVideos.getNextPageToken());
        }
    }

    private void initCardAdapter(final PlaylistVideos playlistVideos) {
        // create the adapter with our playlistVideos and a callback to handle when we reached the last item
        mPlaylistCardAdapter = new PlaylistCardviewAdapter(playlistVideos, new LastItemReachedListener() {
            @Override
            public void onLastItem(int position, String nextPageToken) {
                new PlaylistAsyncTask(mYouTubeDataApi) {
                    @Override
                    public void onPostExecute(Pair<String, List<Video>> result) {
                        handleGetPlaylistResult(playlistVideos, result);
                    }
                }.execute(playlistVideos.playlistId, playlistVideos.getNextPageToken());
            }
        });
        mRecyclerView.setAdapter(mPlaylistCardAdapter);
    }

    private void handleGetPlaylistResult(PlaylistVideos playlistVideos, Pair<String, List<Video>> result) {
        if (result == null) return;
        final int positionStart = playlistVideos.size();
        playlistVideos.setNextPageToken(result.first);
        playlistVideos.addAll(result.second);
        mPlaylistCardAdapter.notifyItemRangeInserted(positionStart, result.second.size());
    }


    /**
     * Interface used by the {@link PlaylistCardviewAdapter} to inform us that we reached the last item in the list.
     */
    public interface LastItemReachedListener {
        void onLastItem(int position, String nextPageToken);
    }

}
