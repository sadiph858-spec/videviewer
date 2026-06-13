package com.videviewer.adapters;

  import android.content.Context;
  import android.view.LayoutInflater;
  import android.view.ViewGroup;
  import androidx.annotation.NonNull;
  import androidx.recyclerview.widget.RecyclerView;
  import com.videviewer.databinding.ItemDownloadBinding;
  import com.videviewer.models.DownloadItem;
  import java.util.List;

  public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {
      private Context context;
      private List<DownloadItem> items;

      public DownloadAdapter(Context context, List<DownloadItem> items) {
          this.context = context;
          this.items = items;
      }

      @NonNull @Override
      public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
          return new DownloadViewHolder(ItemDownloadBinding.inflate(LayoutInflater.from(context), parent, false));
      }

      @Override
      public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
          DownloadItem item = items.get(position);
          holder.binding.tvFilename.setText(item.filename);
          holder.binding.tvStatus.setText(item.getStatusLabel());
          holder.binding.progressBar.setProgress(item.progress);
      }

      @Override public int getItemCount() { return items.size(); }

      static class DownloadViewHolder extends RecyclerView.ViewHolder {
          ItemDownloadBinding binding;
          DownloadViewHolder(ItemDownloadBinding b) { super(b.getRoot()); this.binding = b; }
      }
  }