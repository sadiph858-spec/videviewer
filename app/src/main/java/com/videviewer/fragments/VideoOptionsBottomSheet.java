package com.videviewer.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videviewer.R;
import com.videviewer.activities.VaultActivity;
import com.videviewer.activities.VideoDetailsActivity;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.FavoriteEntity;
import com.videviewer.database.PlaylistEntity;
import com.videviewer.database.PlaylistVideoEntity;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class VideoOptionsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_VIDEO = "arg_video";
    private static final int DELETE_REQUEST = 300;
    private VideoItem video;

    public static VideoOptionsBottomSheet newInstance(VideoItem video) {
        VideoOptionsBottomSheet s = new VideoOptionsBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable(ARG_VIDEO, video);
        s.setArguments(args);
        return s;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) video = getArguments().getParcelable(ARG_VIDEO);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_video_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (video == null) { dismiss(); return; }

        TextView tvTitle = view.findViewById(R.id.tv_video_title);
        if (tvTitle != null) tvTitle.setText(video.getTitle());

        View optDetails   = view.findViewById(R.id.option_details);
        View optShare     = view.findViewById(R.id.option_share);
        View optFavorite  = view.findViewById(R.id.option_favorite);
        View optPlaylist  = view.findViewById(R.id.option_playlist);
        View optVault     = view.findViewById(R.id.option_vault);
        View optRename    = view.findViewById(R.id.option_rename);
        View optDelete    = view.findViewById(R.id.option_delete);
        View optDownload  = view.findViewById(R.id.option_download);

        if (optDetails  != null) optDetails.setOnClickListener(v -> { openDetails(); dismiss(); });
        if (optShare    != null) optShare.setOnClickListener(v -> { shareVideo(); dismiss(); });
        if (optFavorite != null) optFavorite.setOnClickListener(v -> { toggleFavorite(); dismiss(); });
        if (optVault    != null) optVault.setOnClickListener(v -> { moveToVault(); dismiss(); });
        if (optRename   != null) optRename.setOnClickListener(v -> showRenameDialog());
        if (optDelete   != null) optDelete.setOnClickListener(v -> { showDeleteDialog(); dismiss(); });
        if (optDownload != null) optDownload.setOnClickListener(v -> { downloadVideo(); dismiss(); });

        // ── Add to Playlist — bottom sheet stays open while dialog shows ──
        if (optPlaylist != null) {
            optPlaylist.setOnClickListener(v -> showAddToPlaylistDialog());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLAYLIST — Fixed: no early dismiss, sync DB query, dismiss after pick
    // ─────────────────────────────────────────────────────────────────────────

    private void showAddToPlaylistDialog() {
        if (!isAdded()) return;
        // Fetch playlists on background thread, show dialog on UI thread
        // Bottom sheet stays visible until user picks
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
                List<PlaylistEntity> playlists = db.playlistDao().getAllPlaylistsSync();

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (playlists.isEmpty()) {
                        new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("No Playlists")
                            .setMessage("Create a playlist first from the Playlists screen, then add videos here.")
                            .setPositiveButton("Create Now", (d, w) -> {
                                dismiss();
                                try {
                                    startActivity(new Intent(requireContext(),
                                        com.videviewer.activities.PlaylistActivity.class));
                                } catch (Exception e) { e.printStackTrace(); }
                            })
                            .setNegativeButton("Cancel", (d, w) -> dismiss())
                            .show();
                        return;
                    }
                    // Build list: "+ Create new" + existing playlists
                    String[] items = new String[playlists.size() + 1];
                    items[0] = "+ Create new playlist";
                    for (int i = 0; i < playlists.size(); i++) items[i + 1] = playlists.get(i).name;

                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Add to Playlist")
                        .setItems(items, (d, which) -> {
                            if (which == 0) {
                                showCreateAndAddDialog(db);
                            } else {
                                PlaylistEntity chosen = playlists.get(which - 1);
                                addVideoToPlaylist(db, chosen.id, chosen.name);
                                dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", (d, w) -> dismiss())
                        .show();
                });
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showCreateAndAddDialog(AppDatabase db) {
        if (!isAdded()) return;
        EditText et = new EditText(requireContext());
        et.setHint("Playlist name");
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Playlist")
            .setView(et)
            .setPositiveButton("Create & Add", (d, w) -> {
                String name = et.getText() != null ? et.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter a name", Toast.LENGTH_SHORT).show();
                    return;
                }
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        PlaylistEntity p = new PlaylistEntity();
                        p.name = name;
                        p.createdAt = System.currentTimeMillis();
                        p.updatedAt = System.currentTimeMillis();
                        p.coverVideoPath = video.getPath();
                        long id = db.playlistDao().insertPlaylist(p);
                        addVideoToPlaylist(db, id, name);
                    } catch (Exception e) { e.printStackTrace(); }
                });
                dismiss();
            })
            .setNegativeButton("Cancel", (d, w) -> dismiss())
            .show();
    }

    private void addVideoToPlaylist(AppDatabase db, long playlistId, String playlistName) {
        final String vPath = video.getPath() != null ? video.getPath() : "";
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PlaylistVideoEntity pv = new PlaylistVideoEntity();
                pv.playlistId = playlistId;
                pv.videoPath  = vPath;
                pv.addedAt    = System.currentTimeMillis();
                pv.sortOrder  = (int)(System.currentTimeMillis() / 1000);
                db.playlistDao().addVideoToPlaylist(pv);
                // Update cover if empty
                PlaylistEntity pl = db.playlistDao().getPlaylistById(playlistId);
                if (pl != null && (pl.coverVideoPath == null || pl.coverVideoPath.isEmpty())) {
                    pl.coverVideoPath = vPath;
                    pl.updatedAt = System.currentTimeMillis();
                    db.playlistDao().updatePlaylist(pl);
                }
                if (isAdded()) requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(),
                        "Added to \"" + playlistName + "\"", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Failed to add to playlist", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OTHER OPTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void openDetails() {
        try {
            Intent i = new Intent(requireContext(), VideoDetailsActivity.class);
            i.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
            startActivity(i);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void moveToVault() {
        try {
            VaultActivity.moveToVault(requireContext(), video);
            Toast.makeText(requireContext(), "Moving to vault...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void shareVideo() {
        try {
            File file = new File(video.getPath());
            if (!file.exists()) {
                Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Video"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot share this video", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
                boolean isFav = db.favoriteDao().isFavorite(video.getPath());
                if (isFav) {
                    db.favoriteDao().deleteByPath(video.getPath());
                    showToast("Removed from favorites");
                } else {
                    FavoriteEntity fav = new FavoriteEntity();
                    fav.videoPath     = video.getPath();
                    fav.videoTitle    = video.getTitle();
                    fav.videoDuration = video.getDuration();
                    fav.videoSize     = video.getSize();
                    fav.addedAt       = System.currentTimeMillis();
                    db.favoriteDao().insert(fav);
                    showToast("Added to favorites ♥");
                }
            } catch (Exception e) { showToast("Error updating favorites"); }
        });
    }

    private void showRenameDialog() {
        try {
            EditText et = new EditText(requireContext());
            String name = video.getTitle() != null ? video.getTitle() : "";
            et.setText(name);
            et.setSelection(name.length());
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename")
                .setView(et)
                .setPositiveButton("Rename", (d, w) -> {
                    String newName = et.getText() != null ? et.getText().toString().trim() : "";
                    if (newName.isEmpty()) return;
                    File src = new File(video.getPath());
                    String ext = "";
                    int dot = src.getName().lastIndexOf('.');
                    if (dot >= 0) ext = src.getName().substring(dot);
                    File dst = new File(src.getParent(), newName + ext);
                    if (src.renameTo(dst)) {
                        android.media.MediaScannerConnection.scanFile(
                            requireContext(), new String[]{dst.getAbsolutePath()}, null, null);
                        showToast("Renamed");
                    } else {
                        showToast("Rename failed");
                    }
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showDeleteDialog() {
        try {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Video?")
                .setMessage((video.getTitle() != null ? video.getTitle() : "This video") +
                    "\n\nThis will permanently delete the file.")
                .setPositiveButton("Delete", (d, w) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void performDelete() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                List<Uri> uris = new ArrayList<>();
                android.database.Cursor cursor = requireContext().getContentResolver().query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Video.Media._ID},
                    MediaStore.Video.Media.DATA + "=?",
                    new String[]{video.getPath()}, null);
                if (cursor != null && cursor.moveToFirst()) {
                    long id = cursor.getLong(0);
                    uris.add(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id)));
                    cursor.close();
                }
                if (!uris.isEmpty()) {
                    android.app.PendingIntent pi = MediaStore.createDeleteRequest(
                        requireContext().getContentResolver(), uris);
                    startIntentSenderForResult(pi.getIntentSender(), DELETE_REQUEST, null, 0, 0, 0, null);
                    return;
                }
            }
            deleteDirectly();
        } catch (IntentSender.SendIntentException e) {
            deleteDirectly();
        } catch (Exception e) {
            showToast("Delete failed: " + e.getMessage());
        }
    }

    private void deleteDirectly() {
        try {
            boolean deleted = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int rows = requireContext().getContentResolver().delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.DATA + "=?", new String[]{video.getPath()});
                deleted = rows > 0;
            }
            File file = new File(video.getPath());
            if (!deleted && file.exists()) deleted = file.delete();
            showToast(deleted ? "Deleted" : "Delete failed");
        } catch (Exception e) { showToast("Delete failed"); }
    }

    private void downloadVideo() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File src = new File(video.getPath());
                if (!src.exists()) { showToast("File not found"); return; }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues vals = new ContentValues();
                    vals.put(MediaStore.Downloads.DISPLAY_NAME, src.getName());
                    vals.put(MediaStore.Downloads.MIME_TYPE, "video/mp4");
                    vals.put(MediaStore.Downloads.IS_PENDING, 1);
                    ContentResolver cr = requireContext().getContentResolver();
                    Uri col = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    Uri itemUri = cr.insert(col, vals);
                    if (itemUri == null) { showToast("Copy failed"); return; }
                    try (java.io.FileInputStream in = new java.io.FileInputStream(src);
                         java.io.OutputStream out = cr.openOutputStream(itemUri)) {
                        if (out == null) { showToast("Copy failed"); return; }
                        byte[] buf = new byte[65536]; int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                    vals.clear(); vals.put(MediaStore.Downloads.IS_PENDING, 0);
                    cr.update(itemUri, vals, null, null);
                } else {
                    File destDir = android.os.Environment
                        .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    if (destDir != null && !destDir.exists()) destDir.mkdirs();
                    File dest = new File(destDir, src.getName());
                    try (java.io.FileInputStream in = new java.io.FileInputStream(src);
                         java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                        byte[] buf = new byte[65536]; int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                }
                showToast("✅ Saved to Downloads: " + src.getName());
            } catch (Exception e) { showToast("Failed: " + e.getMessage()); }
        });
    }

    private void showToast(String msg) {
        if (isAdded() && getActivity() != null) {
            requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
        }
    }
}
