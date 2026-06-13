package com.videviewer.fragments;

  import android.os.Bundle;
  import android.text.Editable;
  import android.text.TextWatcher;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import androidx.fragment.app.Fragment;
  import androidx.recyclerview.widget.GridLayoutManager;
  import com.videviewer.R;
  import com.videviewer.adapters.VideoAdapter;
  import com.videviewer.databinding.FragmentVideosBinding;
  import com.videviewer.models.VideoItem;
  import com.videviewer.utils.MediaStoreHelper;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.concurrent.Executors;

  public class VideosFragment extends Fragment {
      private FragmentVideosBinding binding;
      private VideoAdapter adapter;
      private List<VideoItem> allVideos = new ArrayList<>();

      @Nullable @Override
      public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
          binding = FragmentVideosBinding.inflate(inflater, container, false);
          return binding.getRoot();
      }

      @Override
      public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
          super.onViewCreated(view, savedInstanceState);
          adapter = new VideoAdapter(requireContext(), new ArrayList<>());
          binding.recyclerVideos.setLayoutManager(new GridLayoutManager(requireContext(), 2));
          binding.recyclerVideos.setAdapter(adapter);

          binding.etSearch.addTextChangedListener(new TextWatcher() {
              @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
              @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterVideos(s.toString()); }
              @Override public void afterTextChanged(Editable s) {}
          });

          loadVideos();
      }

      public void loadVideos() {
          binding.progressBar.setVisibility(View.VISIBLE);
          Executors.newSingleThreadExecutor().execute(() -> {
              List<VideoItem> videos = MediaStoreHelper.getAllVideos(requireContext());
              requireActivity().runOnUiThread(() -> {
                  binding.progressBar.setVisibility(View.GONE);
                  allVideos = videos;
                  adapter.updateList(videos);
                  binding.tvEmpty.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
                  binding.tvVideoCount.setText(videos.size() + " videos");
              });
          });
      }

      private void filterVideos(String query) {
          List<VideoItem> filtered = new ArrayList<>();
          for (VideoItem v : allVideos) {
              if (v.title.toLowerCase().contains(query.toLowerCase())) filtered.add(v);
          }
          adapter.updateList(filtered);
      }

      @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
  }