package com.videviewer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.videviewer.R;
import com.videviewer.models.VideoItem;

/**
 * VideoAdapter - RecyclerView adapter supporting both Grid and List views
 * Uses ListAdapter with DiffUtil for efficient updates
 */
public class VideoAdapter extends ListAdapter<VideoItem, VideoAdapter.VideoViewHolder> {

    public interface OnVideoClickListener {
        void onVideoClick(VideoItem video, int position);
        void onVideoLongClick(VideoItem video, int position);
    }

    private final Context context;
    private boolean isGridMode;
    private OnVideoClickListener listener;

    private static final DiffUtil.ItemCallback<VideoItem> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<VideoItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull VideoItem oldItem, @NonNull VideoItem newItem) {
                return oldItem.getId() == newItem.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull VideoItem oldItem, @NonNull VideoItem newItem) {
                return oldItem.getPath().equals(newItem.getPath())
                    && oldItem.isFavorite() == newItem.isFavorite();
            }
        };

    public VideoAdapter(Context context, boolean isGridMode) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.isGridMode = isGridMode;
    }

    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.listener = listener;
    }

    public void setGridMode(boolean gridMode) {
        this.isGridMode = gridMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGridMode ? R.layout.item_video_grid : R.layout.item_video_list;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = getItem(position);
        holder.bind(video);
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivThumbnail;
        private final TextView tvTitle;
        private final TextView tvDuration;
        private final TextView tvSize;
        private final ImageView ivFavorite;
        private final View badgeHD;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvSize = itemView.findViewById(R.id.tv_size);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            badgeHD = itemView.findViewById(R.id.badge_hd);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_ID && listener != null) {
                    listener.onVideoClick(getItem(pos), pos);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_ID && listener != null) {
                    listener.onVideoLongClick(getItem(pos), pos);
                }
                return true;
            });
        }

        void bind(VideoItem video) {
            tvTitle.setText(video.getTitle());
            tvDuration.setText(video.getFormattedDuration());
            if (tvSize != null) tvSize.setText(video.getFormattedSize());

            // HD badge
            if (badgeHD != null) {
                badgeHD.setVisibility(video.getHeight() >= 720 ? View.VISIBLE : View.GONE);
            }

            // Favorite icon
            if (ivFavorite != null) {
                ivFavorite.setVisibility(video.isFavorite() ? View.VISIBLE : View.GONE);
            }

            // Load thumbnail using Glide
            Glide.with(context)
                .load(video.getPath())
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_video_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .into(ivThumbnail);
        }
    }
}
