package com.example.dex.youtubeplaylist.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dex.youtubeplaylist.R;
import com.example.dex.youtubeplaylist.activities.YoutubePlaylistFragment;
import com.example.dex.youtubeplaylist.model.PlaylistVideos;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatistics;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

public class PlaylistCardviewAdapter extends RecyclerView.Adapter<PlaylistCardviewAdapter.ViewHolder> {

    private static final DecimalFormat sFormatter = new DecimalFormat("#,###,###");
    private final PlaylistVideos mPlaylistVideos;
    private final YoutubePlaylistFragment.LastItemReachedListener mListener;

    public PlaylistCardviewAdapter(PlaylistVideos playlistVideos, YoutubePlaylistFragment.LastItemReachedListener lastItemReachedListener) {
        mPlaylistVideos = playlistVideos;
        mListener = lastItemReachedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflate a card layout
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_cardview_playlist_layout, parent, false);
        // populate the viewholder
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        if (mPlaylistVideos.size() == 0) {
            return;
        }


        final Video video = mPlaylistVideos.get(position);
        final VideoSnippet videoSnippet = video.getSnippet();
        final VideoContentDetails videoContentDetails = video.getContentDetails();
        final VideoStatistics videoStatistics = video.getStatistics();

        holder.mTitleText.setText(videoSnippet.getTitle());
//        holder.mDescriptionText.setText(videoSnippet.getDescription());

        // load the video thumbnail image
        Picasso.with(holder.mContext)
                .load(videoSnippet.getThumbnails().getHigh().getUrl())
                .placeholder(R.drawable.semi_transparent_box_with_border)
                .into(holder.mThumbnailImage);

        // set the click listener to play the video
        holder.mThumbnailImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + video.getId())));
            }
        });

        // create and set the click listener for both the share icon and share text
        View.OnClickListener shareClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Watch \"" + videoSnippet.getTitle() + "\" on YouTube");
                sendIntent.putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=" + video.getId());
                sendIntent.setType("text/plain");
                holder.mContext.startActivity(sendIntent);
            }
        };
        holder.mShareIcon.setOnClickListener(shareClickListener);
        holder.mShareText.setOnClickListener(shareClickListener);

        // set the video duration text
        holder.mDurationText.setText(parseDuration(videoContentDetails.getDuration()));
        // set the video statistics
        holder.mViewCountText.setText(sFormatter.format(videoStatistics.getViewCount()));
        holder.mLikeCountText.setText(sFormatter.format(videoStatistics.getLikeCount()));
        holder.mDislikeCountText.setText(sFormatter.format(videoStatistics.getDislikeCount()));

        if (mListener != null) {
            // get the next playlist page if we're at the end of the current page and we have another page to get
            final String nextPageToken = mPlaylistVideos.getNextPageToken();
            if (!isEmpty(nextPageToken) && position == mPlaylistVideos.size() - 1) {
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onLastItem(position, nextPageToken);
                    }
                });
            }
        }

        holder.mCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Dialog dialog = new Dialog(holder.mContext);
                dialog.setTitle("Comment Box");
                dialog.setContentView(R.layout.commentt_input_layout);

                int width = (int) (holder.mContext.getResources().getDisplayMetrics().widthPixels * 0.95);
                int height = (int) (holder.mContext.getResources().getDisplayMetrics().heightPixels * 0.6);

                dialog.getWindow().setLayout(width, height);
                dialog.show();

                Button postButton = dialog.findViewById(R.id.btnPostComment);
                final EditText commentText = dialog.findViewById(R.id.dialogComment);

                postButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String comment = commentText.getText().toString();
                        if (TextUtils.isEmpty(comment)) {
                            Toast.makeText(v.getContext(), "Enter some Comment", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(v.getContext(), "Dummy Comment Posted", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


            }
        });

    }

    @Override
    public int getItemCount() {
        return mPlaylistVideos.size();
    }


    private boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    private String parseDuration(String in) {
        boolean hasSeconds = in.indexOf('S') > 0;
        boolean hasMinutes = in.indexOf('M') > 0;

        String s;
        if (hasSeconds) {
            s = in.substring(2, in.length() - 1);
        } else {
            s = in.substring(2, in.length());
        }

        String minutes = "0";
        String seconds = "00";

        if (hasMinutes && hasSeconds) {
            String[] split = s.split("M");
            minutes = split[0];
            seconds = split[1];
        } else if (hasMinutes) {
            minutes = s.substring(0, s.indexOf('M'));
        } else if (hasSeconds) {
            seconds = s;
        }

        // pad seconds with a 0 if less than 2 digits
        if (seconds.length() == 1) {
            seconds = "0" + seconds;
        }

        return minutes + ":" + seconds;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final Context mContext;
        public final TextView mTitleText;
        //        public final TextView mDescriptionText;
        public final ImageView mThumbnailImage;
        public final ImageView mShareIcon;
        public final TextView mShareText;
        public final TextView mDurationText;
        public final TextView mViewCountText;
        public final TextView mLikeCountText;
        public final TextView mDislikeCountText;
        public final ImageView mCommentButton;

        public ViewHolder(View itemView) {
            super(itemView);

            mContext = itemView.getContext();
            mTitleText = itemView.findViewById(R.id.video_title);
//            mDescriptionText = itemView.findViewById(R.id.video_description);
            mThumbnailImage = itemView.findViewById(R.id.video_thumbnail);
            mShareIcon = itemView.findViewById(R.id.video_share);
            mShareText = itemView.findViewById(R.id.video_share_text);
            mDurationText = itemView.findViewById(R.id.video_dutation_text);
            mViewCountText = itemView.findViewById(R.id.video_view_count);
            mLikeCountText = itemView.findViewById(R.id.video_like_count);
            mDislikeCountText = itemView.findViewById(R.id.video_dislike_count);
            mCommentButton = itemView.findViewById(R.id.video_comment);

        }
    }
}
