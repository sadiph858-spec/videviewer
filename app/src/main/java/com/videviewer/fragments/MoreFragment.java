package com.videviewer.fragments;

  import android.content.Intent;
  import android.os.Bundle;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import androidx.fragment.app.Fragment;
  import com.videviewer.activities.VaultActivity;
  import com.videviewer.activities.SettingsActivity;
  import com.videviewer.databinding.FragmentMoreBinding;

  public class MoreFragment extends Fragment {
      private FragmentMoreBinding binding;

      @Nullable @Override
      public View onCreateView// Download
view.findViewById(R.id.card_download).setOnClickListener(v ->
    startActivity(new Intent(requireContext(), 
        com.videviewer.activities.DownloadActivity.class)));(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
          binding = FragmentMoreBinding.inflate(inflater, container, false);
          return binding.getRoot();
      }

      @Override
      public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
          super.onViewCreated(view, savedInstanceState);
          binding.btnVault.setOnClickListener(v -> startActivity(new Intent(requireContext(), VaultActivity.class)));
          binding.btnSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
          binding.btnAbout.setOnClickListener(v -> showAbout());
      }

      private void showAbout() {
          new android.app.AlertDialog.Builder(requireContext(), com.videviewer.R.style.DarkDialogTheme)
              .setTitle("VidViewer v3.0.0")
              .setMessage("Premium Android Video Player\n\nSupports MP4, MKV, AVI, MOV, 3GP, FLV, M4V, WMV, RMVB, TS, MPEG\n\nBuilt with ExoPlayer + FFmpeg")
              .setPositiveButton("OK", null)
              .show();
      }

      @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
  }
