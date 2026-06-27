package com.videviewer.adapters;

  import android.content.Context;
  import android.content.Intent;
  import android.view.LayoutInflater;
  import android.view.ViewGroup;
  import android.widget.Toast;
  import androidx.annotation.NonNull;
  import androidx.recyclerview.widget.RecyclerView;
  import com.bumptech.glide.Glide;
  import com.videviewer.R;
  import com.videviewer.databinding.ItemStatusBinding;
  import com.videviewer.models.VideoItem;
  import java.io.File;
  import java.util.ArrayList;
  import java.util.List;

  public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.StatusViewHolder> {
      private Context context;
      private List<VideoItem> items;

      public StatusAdapter(Context context, List<VideoItem> items) {
          this.context = context;
          this.items = items;
      }

      public void updateList(List<VideoItem> newList) {
          this.items = new ArrayList<>(newList);
          notifyDataSetChanged();
      }

      @NonNull @Override
      public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
          return new StatusViewHolder(ItemStatusBinding.inflate(LayoutInflater.from(context), parent, false));
      }

      @Override
      public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
          VideoItem item = items.get(position);
          Glide.with(context).load(new File(item.path)).placeholder(R.drawable.ic_video_placeholder).into(holder.binding.ivThumbnail);
          boolean isVideo = item.path.endsWith(".mp4");
          holder.binding.ivPlayOverlay.setVisibility(isVideo ? android.view.View.VISIBLE : android.view.View.GONE);
          holder.binding.btnSave.setOnClickListener(v -> {
              com.videviewer.utils.FileUtils.saveToGallery(context, item.path);
              Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show();
          });
      }

      @Override public int getItemCount() { return items.size(); }

      static class StatusViewHolder extends RecyclerView.ViewHolder {
          ItemStatusBinding binding;
          StatusViewHolder(ItemStatusBinding b) { super(b.getRoot()); this.binding = b; }
      }
  }