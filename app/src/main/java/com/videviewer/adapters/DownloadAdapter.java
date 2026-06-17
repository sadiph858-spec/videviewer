package com.videviewer.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.videviewer.databinding.ItemDownloadBinding;
import com.videviewer.models.DownloadItem;
import java.io.File;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {

    private final Context context;
    private final List<DownloadItem> items;

    public interface OnItemClickListener  { void onItemClick(DownloadItem item); }
    public interface OnItemDeleteListener { void onItemDelete(int position, DownloadItem item); }

    private OnItemClickListener  clickListener;
    private OnItemDeleteListener deleteListener;

    public void setOnItemClickListener(OnItemClickListener l)   { this.clickListener  = l; }
    public void setOnItemDeleteListener(OnItemDeleteListener l) { this.deleteListener = l; }

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

        boolean completed   = item.status == DownloadItem.STATUS_COMPLETED;
        boolean downloading = item.status == DownloadItem.STATUS_DOWNLOADING;

        // Progress bar
        holder.binding.progressBar.setVisibility(downloading ? View.VISIBLE : View.GONE);
        holder.binding.progressBar.setProgress(item.progress);

        // Thumbnail logic
        if (completed && item.filePath != null && !item.filePath.isEmpty()) {
            // Local file frame
            holder.binding.ivThumbnail.setVisibility(View.VISIBLE);
            holder.binding.ivIcon.setVisibility(View.GONE);
            Glide.with(context)
                .asBitmap()
                .load(Uri.fromFile(new File(item.filePath)))
                .apply(RequestOptions.frameOf(1_000_000L))
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(holder.binding.ivThumbnail);
        } else if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
            // Remote thumbnail (YouTube etc.)
            holder.binding.ivThumbnail.setVisibility(View.VISIBLE);
            holder.binding.ivIcon.setVisibility(View.GONE);
            Glide.with(context)
                .load(item.thumbnailUrl)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(holder.binding.ivThumbnail);
        } else {
            holder.binding.ivThumbnail.setVisibility(View.GONE);
            holder.binding.ivIcon.setVisibility(View.VISIBLE);
        }

        // Tap → play
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(item);
        });

        // Long press → delete dialog
        holder.itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                .setTitle("Delete download?")
                .setMessage(item.filename)
                .setPositiveButton("Delete", (d, w) -> {
                    if (deleteListener != null)
                        deleteListener.onItemDelete(holder.getAdapterPosition(), item);
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {
        final ItemDownloadBinding binding;
        DownloadViewHolder(ItemDownloadBinding b) { super(b.getRoot()); this.binding = b; }
    }
}
