package com.videviewer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.VideoDecoder;
import com.bumptech.glide.request.RequestOptions;
import com.videviewer.databinding.ItemDownloadBinding;
import com.videviewer.models.DownloadItem;
import java.io.File;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {

    private final Context context;
    private final List<DownloadItem> items;

    public interface OnItemClickListener {
        void onItemClick(DownloadItem item);
    }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public DownloadAdapter(Context context, List<DownloadItem> items) {
        this.context = context;
        this.items   = items;
    }

    @NonNull @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DownloadViewHolder(
            ItemDownloadBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem item = items.get(position);
        holder.binding.tvFilename.setText(item.filename);
        holder.binding.tvStatus.setText(item.getStatusLabel());

        boolean completed = item.status == DownloadItem.STATUS_COMPLETED;

        // Progress bar — only visible while downloading
        holder.binding.progressBar.setVisibility(
            item.status == DownloadItem.STATUS_DOWNLOADING ? View.VISIBLE : View.GONE);
        holder.binding.progressBar.setProgress(item.progress);

        if (completed && item.filePath != null && !item.filePath.isEmpty()) {
            // Show thumbnail
            holder.binding.ivThumbnail.setVisibility(View.VISIBLE);
            holder.binding.ivIcon.setVisibility(View.GONE);
            Glide.with(context)
                .asBitmap()
                .load(Uri.fromFile(new File(item.filePath)))
                .apply(RequestOptions.frameOf(1_000_000L))  // 1-second frame
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(holder.binding.ivThumbnail);
        } else {
            holder.binding.ivThumbnail.setVisibility(View.GONE);
            holder.binding.ivIcon.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {
        final ItemDownloadBinding binding;
        DownloadViewHolder(ItemDownloadBinding b) { super(b.getRoot()); this.binding = b; }
    }
}
