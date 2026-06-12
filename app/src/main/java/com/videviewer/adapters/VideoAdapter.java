package com.videviewer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

    public interface OnVideoMenuClickListener {
        void onMenuClick(VideoItem video, int position, View anchor);
    }

    private final Context context;
    private boolean isGridMode;
    private OnVideoClickListener listener;
    private OnVideoMenuClickListener menuListener;

    private static final DiffUtil.ItemCallback<VideoItem> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<VideoItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull VideoItem oldItem, @NonNull VideoItem newItem) {
                return oldItem.getId() == newItem.getId()
                    || (oldItem.getPath() != null && oldItem.getPath().equals(newItem.getPath()));
            }
            @Override
            public boolean areContentsTheSame(@NonNull VideoItem oldItem, @NonNull VideoItem newItem) {
                return oldItem.getPath() != null && oldItem.getPath().equals(newItem.getPath())
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

    public void setOnVideoMenuClickListener(OnVideoMenuClickListener listener) {
        this.menuListener = listener;
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
        try {
            VideoItem video = getItem(position);
            holder.bind(video);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivThumbnail;
        private final TextView tvTitle;
        private final TextView tvDuration;
        private final TextView tvSize;
        private final TextView tvFolder;
        private final TextView tvFormat;
        private final ImageView ivFavorite;
        private final View badgeHD;
        private final ImageButton btnMore;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvSize = itemView.findViewById(R.id.tv_size);
            tvFolder = itemView.findViewById(R.id.tv_folder);
            tvFormat = itemView.findViewById(R.id.tv_format);
            ivFavorite = itemView.findViewById(R.id.iv_favorite);
            badgeHD = itemView.findViewById(R.id.badge_hd);
            btnMore = itemView.findViewById(R.id.btn_more);

            itemView.setOnClickListener(v -> {
                try {
                    int pos = getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_ID && listener != null) {
                        listener.onVideoClick(getItem(pos), pos);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            itemView.setOnLongClickListener(v -> {
                try {
                    int pos = getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_ID && listener != null) {
                        listener.onVideoLongClick(getItem(pos), pos);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            });

            if (btnMore != null) {
                btnMore.setOnClickListener(v -> {
                    try {
                        int pos = getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_ID) {
                            if (menuListener != null) {
                                menuListener.onMenuClick(getItem(pos), pos, v);
                            } else if (listener != null) {
                                listener.onVideoLongClick(getItem(pos), pos);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        void bind(VideoItem video) {
            try {
                if (tvTitle != null) tvTitle.setText(video.getTitle());
                if (tvDuration != null) tvDuration.setText(video.getFormattedDuration());
                if (tvSize != null) tvSize.setText(video.getFormattedSize());

                // Folder name
                if (tvFolder != null) {
                    String folder = video.getFolderName();
                    if (folder != null && !folder.isEmpty()) {
                        tvFolder.setText(folder);
                        tvFolder.setVisibility(View.VISIBLE);
                    } else {
                        tvFolder.setVisibility(View.GONE);
                    }
                }

                // Format badge from MIME type
                if (tvFormat != null) {
                    String fmt = getFormatLabel(video.getMimeType());
                    if (fmt != null) {
                        tvFormat.setText(fmt);
                        tvFormat.setVisibility(View.VISIBLE);
                    } else {
                        tvFormat.setVisibility(View.GONE);
                    }
                }

                // HD badge
                if (badgeHD != null) {
                    badgeHD.setVisibility(video.getHeight() >= 720 ? View.VISIBLE : View.GONE);
                }

                // Favorite icon
                if (ivFavorite != null) {
                    ivFavorite.setVisibility(video.isFavorite() ? View.VISIBLE : View.GONE);
                }

                // Load thumbnail using Glide
                if (ivThumbnail != null) {
                    Glide.with(context)
                        .load(video.getPath())
                        .placeholder(R.drawable.ic_video_placeholder)
                        .error(R.drawable.ic_video_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()
                        .into(ivThumbnail);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String getFormatLabel(String mimeType) {
            if (mimeType == null) return null;
            switch (mimeType) {
                case "video/mp4": return "MP4";
                case "video/x-matroska": return "MKV";
                case "video/x-msvideo": return "AVI";
                case "video/3gpp": return "3GP";
                case "video/webm": return "WEBM";
                case "video/quicktime": return "MOV";
                case "video/mpeg": return "MPEG";
                case "video/x-flv": return "FLV";
                case "video/ts": return "TS";
                default:
                    if (mimeType.startsWith("video/")) {
                        String sub = mimeType.substring(6).toUpperCase();
                        if (sub.length() > 5) sub = sub.substring(0, 5);
                        return sub;
                    }
                    return null;
            }
        }
    }
}
