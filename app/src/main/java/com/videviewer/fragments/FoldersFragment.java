package com.videviewer.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.videviewer.R;
import com.videviewer.activities.FolderActivity;
import com.videviewer.adapters.FolderAdapter;
import com.videviewer.models.FolderItem;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VideoScanner;
import java.util.ArrayList;
import java.util.List;

/**
 * FoldersFragment - Displays video folders grouped from device storage
 */
public class FoldersFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private FolderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_folders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.rv_folders);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvEmpty = view.findViewById(R.id.tv_empty);

        adapter = new FolderAdapter(requireContext());
        adapter.setOnFolderClickListener(folder -> {
            Intent intent = new Intent(requireContext(), FolderActivity.class);
            intent.putExtra(AppConstants.EXTRA_FOLDER_PATH, folder.getFolderPath());
            intent.putExtra("folder_name", folder.getFolderName());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadFolders);
        swipeRefresh.setColorSchemeResources(R.color.md_theme_primary);
        loadFolders();
    }

    private void loadFolders() {
        new AsyncTask<Void, Void, List<FolderItem>>() {
            @Override
            protected void onPreExecute() { swipeRefresh.setRefreshing(true); }

            @Override
            protected List<FolderItem> doInBackground(Void... voids) {
                VideoScanner scanner = new VideoScanner(requireContext());
                List<VideoItem> all = scanner.scanAllVideos();
                return scanner.getFolders(all);
            }

            @Override
            protected void onPostExecute(List<FolderItem> folders) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                adapter.submitList(folders);
                tvEmpty.setVisibility(folders.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }.execute();
    }
}
