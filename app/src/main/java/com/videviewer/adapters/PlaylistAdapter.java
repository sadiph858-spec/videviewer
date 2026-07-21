package com.videviewer.adapters;

import android.content.Context;
import android.net.Uri;
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
import com.videviewer.R;
import com.videviewer.database.PlaylistEntity;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PlaylistAdapter extends ListAdapter<PlaylistEntity, PlaylistAdapter.ViewHolder> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(PlaylistEntity playlist);
        void onPlaylistLongClick(PlaylistEntity playlist);
    }

    private final Context context;
    private OnPlaylistClickListener listener;
    private final Map<Long, Integer> videoCounts = new HashMap<>();

    private static final DiffUtil.ItemCallback<PlaylistEntity> DIFF =
        new DiffUtil.ItemCallback<PlaylistEntity>() {
            @Override public boolean areItemsTheSame(@NonNull PlaylistEntity a, @NonNull PlaylistEntity b) { return a.id == b.id; }
            @Override public boolean areContentsTheSame(@NonNull PlaylistEntity a, @NonNull PlaylistEntity b) {
                return a.id == b.id && a.name != null && a.name.equals(b.name);
            }
        };

    public PlaylistAdapter(Context context) { super(DIFF); this.context = context; }
    public void setListener(OnPlaylistClickListener l) { this.listener = l; }

    public void setVideoCount(long playlistId, int count) {
        videoCounts.put(playlistId, count);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        PlaylistEntity p = getItem(pos);
        h.tvName.setText(p.name);
        int cnt = videoCounts.containsKey(p.id) ? videoCounts.get(p.id) : 0;
        h.tvCount.setText(cnt + " video" + (cnt == 1 ? "" : "s"));

        if (p.coverVideoPath != null && !p.coverVideoPath.isEmpty() && new File(p.coverVideoPath).exists()) {
            Glide.with(context).asBitmap()
                .load(Uri.fromFile(new File(p.coverVideoPath)))
                .apply(com.bumptech.glide.request.RequestOptions.frameOf(1_000_000L))
                .placeholder(android.R.color.darker_gray).error(android.R.color.darker_gray)
                .into(h.ivCover);
        } else {
            Glide.with(context).clear(h.ivCover);
            h.ivCover.setImageResource(R.drawable.ic_video_placeholder);
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onPlaylistClick(p); });
        h.itemView.setOnLongClickListener(v -> { if (listener != null) listener.onPlaylistLongClick(p); return true; });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover; TextView tvName, tvCount;
        ViewHolder(View v) { super(v); ivCover = v.findViewById(R.id.iv_playlist_cover); tvName = v.findViewById(R.id.tv_playlist_name); tvCount = v.findViewById(R.id.tv_video_count); }
    }
}
