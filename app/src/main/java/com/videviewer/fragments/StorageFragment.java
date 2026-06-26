package com.videviewer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.videviewer.adapters.StatusAdapter;
import com.videviewer.databinding.FragmentStorageBinding;
import com.videviewer.models.VideoItem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class StorageFragment extends Fragment {
    private FragmentStorageBinding binding;
    private StatusAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStorageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new StatusAdapter(requireContext(), new ArrayList<>());
        binding.recyclerStatus.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.recyclerStatus.setAdapter(adapter);
        binding.tabLayout.addOnTabSelectedListener(
            new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) loadWhatsAppStatuses();
                    else loadFolderBrowser();
                }
                @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            });
        loadWhatsAppStatuses();
    }

    private void loadWhatsAppStatuses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> statuses = new ArrayList<>();
            String[] waPaths = {
                Environment.getExternalStorageDirectory() + "/WhatsApp/Media/.Statuses",
                Environment.getExternalStorageDirectory() + "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                Environment.getExternalStorageDirectory() + "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
            };
            for (String path : waPaths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.getName().endsWith(".mp4")
                                    || f.getName().endsWith(".jpg")
                                    || f.getName().endsWith(".jpeg")) {
                                VideoItem item = new VideoItem();
                                item.setPath(f.getAbsolutePath());
                                item.setTitle(f.getName());
                                item.setSize(f.length());
                                statuses.add(item);
                            }
                        }
                    }
                }
            }
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                adapter.updateList(statuses);
                binding.tvEmpty.setVisibility(statuses.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void loadFolderBrowser() {
        // Placeholder — show external storage files
        binding.progressBar.setVisibility(View.VISIBLE);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<VideoItem> files = new ArrayList<>();
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads.exists()) {
                File[] list = downloads.listFiles();
                if (list != null) {
                    for (File f : list) {
                        if (!f.isFile()) continue;
                        String n = f.getName().toLowerCase();
                        if (n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".webm")) {
                            VideoItem item = new VideoItem();
                            item.setPath(f.getAbsolutePath());
                            item.setTitle(f.getName());
                            item.setSize(f.length());
                            files.add(item);
                        }
                    }
                }
            }
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                adapter.updateList(files);
                binding.tvEmpty.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
