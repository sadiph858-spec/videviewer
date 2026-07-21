package com.videviewer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.videviewer.activities.FolderActivity;
import com.videviewer.adapters.FolderAdapter;
import com.videviewer.adapters.StatusAdapter;
import com.videviewer.databinding.FragmentStorageBinding;
import com.videviewer.models.FolderItem;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class StorageFragment extends Fragment {
    private FragmentStorageBinding binding;
    private StatusAdapter statusAdapter;
    private FolderAdapter folderAdapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStorageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusAdapter = new StatusAdapter(requireContext(), new ArrayList<>());
        folderAdapter = new FolderAdapter(requireContext());
        folderAdapter.setOnFolderClickListener(folder -> {
            Intent i = new Intent(requireContext(), FolderActivity.class);
            i.putExtra(AppConstants.EXTRA_FOLDER_PATH, folder.getFolderPath());
            i.putExtra("folder_name", folder.getFolderName());
            startActivity(i);
        });

        binding.tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
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
        binding.recyclerStatus.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.recyclerStatus.setAdapter(statusAdapter);

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
                            String n = f.getName().toLowerCase();
                            if (n.endsWith(".mp4") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) {
                                VideoItem item = new VideoItem();
                                item.path  = f.getAbsolutePath();
                                item.title = f.getName();
                                item.size  = f.length();
                                statuses.add(item);
                            }
                        }
                    }
                }
            }
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                statusAdapter.updateList(statuses);
                binding.tvEmpty.setVisibility(statuses.isEmpty() ? View.VISIBLE : View.GONE);
                if (statuses.isEmpty()) {
                    binding.tvEmpty.setText("No WhatsApp statuses found.\nWhatsApp statuses will appear here.");
                }
            });
        });
    }

    private void loadFolderBrowser() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerStatus.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerStatus.setAdapter(folderAdapter);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<FolderItem> folders = new ArrayList<>();
            try {
                // Scan all video files and group by folder
                android.net.Uri uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                String[] proj = {
                    android.provider.MediaStore.Video.Media.DATA,
                    android.provider.MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    android.provider.MediaStore.Video.Media.BUCKET_ID,
                    android.provider.MediaStore.Video.Media.SIZE
                };
                try (android.database.Cursor c = requireContext().getContentResolver()
                        .query(uri, proj, null, null,
                               android.provider.MediaStore.Video.Media.DATE_ADDED + " DESC")) {
                    if (c != null) {
                        Map<String, FolderItem> folderMap = new HashMap<>();
                        int dataCol   = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATA);
                        int nameCol   = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                        int sizeCol   = c.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE);
                        while (c.moveToNext()) {
                            String path   = c.getString(dataCol);
                            String bucket = c.getString(nameCol);
                            long   size   = c.getLong(sizeCol);
                            if (path == null || bucket == null) continue;
                            String folderPath = path.substring(0, path.lastIndexOf('/'));
                            if (folderMap.containsKey(folderPath)) {
                                FolderItem fi = folderMap.get(folderPath);
                                fi.setVideoCount(fi.getVideoCount() + 1);
                                fi.setTotalSize(fi.getTotalSize() + size);
                            } else {
                                FolderItem fi = new FolderItem();
                                fi.setFolderPath(folderPath);
                                fi.setFolderName(bucket);
                                fi.setVideoCount(1);
                                fi.setTotalSize(size);
                                fi.setCoverPath(path);
                                folderMap.put(folderPath, fi);
                            }
                        }
                        folders.addAll(folderMap.values());
                        // Sort by video count desc
                        folders.sort((a, b) -> b.getVideoCount() - a.getVideoCount());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isAdded()) return;
            List<FolderItem> finalFolders = folders;
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                folderAdapter.submitList(finalFolders);
                binding.tvEmpty.setVisibility(finalFolders.isEmpty() ? View.VISIBLE : View.GONE);
                if (finalFolders.isEmpty()) {
                    binding.tvEmpty.setText("No video folders found.");
                }
            });
        });
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
