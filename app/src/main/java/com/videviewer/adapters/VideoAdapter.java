package com.videviewer.adapters;

  import android.content.Context;
  import android.content.Intent;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import android.widget.ImageButton;
  import android.widget.ImageView;
  import android.widget.PopupMenu;
  import android.widget.TextView;
  import android.widget.Toast;
  import androidx.annotation.NonNull;
  import androidx.recyclerview.widget.RecyclerView;
  import com.bumptech.glide.Glide;
  import com.bumptech.glide.load.engine.DiskCacheStrategy;
  import com.videviewer.R;
  import com.videviewer.activities.PlayerActivity;
  import com.videviewer.databinding.ItemVideoBinding;
  import com.videviewer.models.VideoItem;
  import com.videviewer.utils.AppConstants;
  import java.io.File;
  import java.util.ArrayList;
  import java.util.List;

  public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

      private Context context;
      private List<VideoItem> videos;

      
      public interface OnVideoClickListener {
          void onVideoClick(VideoItem video, int position);
          default void onVideoLongClick(VideoItem video, int position) {}
      }

      private OnVideoClickListener clickListener;

      public void setOnVideoClickListener(OnVideoClickListener listener) {
          this.clickListener = listener;
      }

      private boolean isGridMode;

        public VideoAdapter(Context context, List<VideoItem> videos) {
            this.context = context;
            this.videos = (videos != null) ? videos : new ArrayList<>();
        }

        /** Constructor with grid/list mode flag */
        public VideoAdapter(Context context, boolean isGridMode) {
            this.context = context;
            this.videos = new ArrayList<>();
            this.isGridMode = isGridMode;
        }

      public void updateList(List<VideoItem> newList) {
            this.videos = (newList != null) ? new ArrayList<>(newList) : new ArrayList<>();
            notifyDataSetChanged();
        }

        public void submitList(List<VideoItem> newList) {
            this.videos = (newList != null) ? new ArrayList<>(newList) : new ArrayList<>();
            notifyDataSetChanged();
        }

      @NonNull @Override
      public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
          ItemVideoBinding binding = ItemVideoBinding.inflate(LayoutInflater.from(context), parent, false);
          return new VideoViewHolder(binding);
      }

      @Override
      public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
          VideoItem item = videos.get(position);
          holder.binding.tvTitle.setText(item.title);
          holder.binding.tvFolder.setText(item.folder);
          holder.binding.tvDuration.setText(item.getFormattedDuration());
          holder.binding.tvSize.setText(item.getFormattedSize());
          holder.binding.tvFormat.setText(item.getExtension());

          Glide.with(context)
              .load(new File(item.path))
              .diskCacheStrategy(DiskCacheStrategy.ALL)
              .placeholder(R.drawable.ic_video_placeholder)
              .thumbnail(0.1f)
              .into(holder.binding.ivThumbnail);

          holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onVideoClick(item, position);
                } else {
                    Intent intent = new Intent(context, PlayerActivity.class);
                    intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, item.path);
                    intent.putExtra(AppConstants.EXTRA_VIDEO_TITLE, item.title);
                    context.startActivity(intent);
                }
            });

          holder.binding.btnMenu.setOnClickListener(v -> showPopupMenu(v, item, position));
      }

      private void showPopupMenu(View anchor, VideoItem item, int position) {
          PopupMenu popup = new PopupMenu(context, anchor);
          popup.getMenuInflater().inflate(R.menu.menu_video_item, popup.getMenu());
          popup.setOnMenuItemClickListener(menuItem -> {
              int id = menuItem.getItemId();
              if (id == R.id.action_play) {
                  Intent intent = new Intent(context, PlayerActivity.class);
                  intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, item.path);
                  intent.putExtra(AppConstants.EXTRA_VIDEO_TITLE, item.title);
                  context.startActivity(intent);
              } else if (id == R.id.action_share) {
                  com.videviewer.utils.FileUtils.shareVideo(context, item.path);
              } else if (id == R.id.action_delete) {
                  Toast.makeText(context, "Delete: " + item.title, Toast.LENGTH_SHORT).show();
              } else if (id == R.id.action_details) {
                  showDetails(item);
              } else if (id == R.id.action_favorite) {
                  Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
              } else if (id == R.id.action_vault) {
                  Toast.makeText(context, "Moved to vault", Toast.LENGTH_SHORT).show();
              }
              return true;
          });
          popup.show();
      }

      private void showDetails(VideoItem item) {
          new android.app.AlertDialog.Builder(context, R.style.DarkDialogTheme)
              .setTitle("Video Details")
              .setMessage("Title: " + item.title + "\nPath: " + item.path + "\nSize: " + item.getFormattedSize() + "\nDuration: " + item.getFormattedDuration() + "\nFormat: " + item.getExtension())
              .setPositiveButton("OK", null)
              .show();
      }

      @Override public int getItemCount() { return videos.size(); }

      static class VideoViewHolder extends RecyclerView.ViewHolder {
          ItemVideoBinding binding;
          VideoViewHolder(ItemVideoBinding binding) { super(binding.getRoot()); this.binding = binding; }
      }
  }