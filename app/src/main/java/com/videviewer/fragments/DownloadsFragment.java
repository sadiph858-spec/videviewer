package com.videviewer.fragments;

  import android.content.Intent;
  import android.os.Bundle;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import androidx.fragment.app.Fragment;
  import androidx.recyclerview.widget.LinearLayoutManager;
  import com.videviewer.adapters.DownloadAdapter;
  import com.videviewer.databinding.FragmentDownloadsBinding;
  import com.videviewer.models.DownloadItem;
  import com.videviewer.services.DownloadService;
  import java.util.ArrayList;
  import java.util.List;

  public class DownloadsFragment extends Fragment {
      private FragmentDownloadsBinding binding;
      private DownloadAdapter adapter;
      private List<DownloadItem> downloadList = new ArrayList<>();

      @Nullable @Override
      public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
          binding = FragmentDownloadsBinding.inflate(inflater, container, false);
          return binding.getRoot();
      }

      @Override
      public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
          super.onViewCreated(view, savedInstanceState);
          adapter = new DownloadAdapter(requireContext(), downloadList);
          binding.recyclerDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
          binding.recyclerDownloads.setAdapter(adapter);

          binding.btnAddDownload.setOnClickListener(v -> showAddDownloadDialog());
          binding.tvEmpty.setVisibility(downloadList.isEmpty() ? View.VISIBLE : View.GONE);
      }

      private void showAddDownloadDialog() {
          android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext(), com.videviewer.R.style.DarkDialogTheme);
          builder.setTitle("Add Download");
          final android.widget.EditText et = new android.widget.EditText(requireContext());
          et.setHint("Paste video URL (.mp4, .mkv, .webm)");
          et.setTextColor(0xFFFFFFFF);
          et.setHintTextColor(0xFF888888);
          builder.setView(et);
          builder.setPositiveButton("Download", (d, w) -> {
              String url = et.getText().toString().trim();
              if (!url.isEmpty()) startDownload(url);
          });
          builder.setNegativeButton("Cancel", null);
          builder.show();
      }

      private void startDownload(String url) {
          Intent intent = new Intent(requireContext(), DownloadService.class);
          intent.putExtra("url", url);
          requireContext().startForegroundService(intent);
          DownloadItem item = new DownloadItem(url, 0, DownloadItem.STATUS_DOWNLOADING);
          downloadList.add(0, item);
          adapter.notifyItemInserted(0);
          binding.tvEmpty.setVisibility(View.GONE);
      }

      @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
  }