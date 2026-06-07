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
import com.videviewer.models.FolderItem;

/**
 * FolderAdapter - RecyclerView adapter for folder grid view
 */
public class FolderAdapter extends ListAdapter<FolderItem, FolderAdapter.FolderViewHolder> {

    public interface OnFolderClickListener {
        void onFolderClick(FolderItem folder);
    }

    private final Context context;
    private OnFolderClickListener listener;

    private static final DiffUtil.ItemCallback<FolderItem> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<FolderItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull FolderItem o, @NonNull FolderItem n) {
                return o.getFolderPath().equals(n.getFolderPath());
            }
            @Override
            public boolean areContentsTheSame(@NonNull FolderItem o, @NonNull FolderItem n) {
                return o.getVideoCount() == n.getVideoCount();
            }
        };

    public FolderAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    public void setOnFolderClickListener(OnFolderClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class FolderViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvFolderName;
        private final TextView tvVideoCount;
        private final TextView tvSize;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_folder_thumbnail);
            tvFolderName = itemView.findViewById(R.id.tv_folder_name);
            tvVideoCount = itemView.findViewById(R.id.tv_video_count);
            tvSize = itemView.findViewById(R.id.tv_folder_size);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_ID && listener != null) {
                    listener.onFolderClick(getItem(pos));
                }
            });
        }

        void bind(FolderItem folder) {
            tvFolderName.setText(folder.getFolderName());
            tvVideoCount.setText(folder.getVideoCountText());
            if (tvSize != null) tvSize.setText(folder.getFormattedSize());

            if (folder.getCoverVideoPath() != null) {
                Glide.with(context)
                    .load(folder.getCoverVideoPath())
                    .placeholder(R.drawable.ic_video_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
                    .into(ivThumbnail);
            } else {
                ivThumbnail.setImageResource(R.drawable.ic_video_placeholder);
            }
        }
    }
}
