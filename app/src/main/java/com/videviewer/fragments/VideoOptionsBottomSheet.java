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

        view.findViewById(R.id.option_details).setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), VideoDetailsActivity.class);
            i.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
            startActivity(i); dismiss();
        });
        view.findViewById(R.id.option_share).setOnClickListener(v -> { shareVideo(); dismiss(); });
        view.findViewById(R.id.option_favorite).setOnClickListener(v -> { toggleFavorite(); dismiss(); });
        view.findViewById(R.id.option_rename).setOnClickListener(v -> showRenameDialog());
        view.findViewById(R.id.option_delete).setOnClickListener(v -> { showDeleteDialog(); dismiss(); });
        view.findViewById(R.id.option_vault).setOnClickListener(v -> {
            VaultActivity.moveToVault(requireContext(), video);
            Toast.makeText(requireContext(), "Moving to vault...", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        view.findViewById(R.id.option_download).setOnClickListener(v -> { downloadVideo(); dismiss(); });

        // ── PLAYLIST (was "coming soon" — now fully functional) ─────────────
        view.findViewById(R.id.option_playlist).setOnClickListener(v -> {
            showAddToPlaylistDialog();
        });
    }

    // ── Add to Playlist ───────────────────────────────────────────────────────

    private void showAddToPlaylistDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<PlaylistEntity> playlists = new ArrayList<>();
                // Get snapshot synchronously
                db.playlistDao().getAllPlaylists().getValue();
                // Use a direct query workaround — getAllPlaylists is LiveData, do a sync fetch
                // by reading via Room on this executor thread:
                List<PlaylistEntity> allLists = fetchPlaylistsSync(db);

                requireActivity().runOnUiThread(() -> {
                    try {
                        if (allLists.isEmpty()) {
                            // No playlists — offer to create one
                            new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("No Playlists")
                                .setMessage("You don\'t have any playlists yet. Create one?")
                                .setPositiveButton("Create", (d, w) -> showCreateAndAddDialog(db))
                                .setNegativeButton("Cancel", null)
                                .show();
                            return;
                        }

                        // Build display list with "+ Create new" at top
                        String[] items = new String[allLists.size() + 1];
                        items[0] = "+ Create new playlist";
                        for (int i = 0; i < allLists.size(); i++) items[i + 1] = allLists.get(i).name;

                        new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Add to Playlist")
                            .setItems(items, (d, which) -> {
                                if (which == 0) {
                                    showCreateAndAddDialog(db);
                                } else {
                                    PlaylistEntity chosen = allLists.get(which - 1);
                                    addVideoToPlaylist(db, chosen.id, chosen.name);
                                }
                            })
                            .show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Error loading playlists", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
        dismiss();
    }

    private List<PlaylistEntity> fetchPlaylistsSync(AppDatabase db) {
        // Room doesn\'t support direct sync on non-main thread via LiveData,
        // but we can query via a @Query returning List directly.
        // PlaylistDao has getAllPlaylists() as LiveData. We need a workaround.
        // Use the existing insertPlaylist to count — actually, let\'s use
        // a helper method via reflection or just query the LiveData on the main thread.
        // Simplest: observe for one shot. Here we use a CountDownLatch approach.
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final List<PlaylistEntity> result = new ArrayList<>();
        requireActivity().runOnUiThread(() -> {
            db.playlistDao().getAllPlaylists().observeForever(new androidx.lifecycle.Observer<List<PlaylistEntity>>() {
                @Override public void onChanged(List<PlaylistEntity> list) {
                    if (list != null) result.addAll(list);
                    db.playlistDao().getAllPlaylists().removeObserver(this);
                    latch.countDown();
                }
            });
        });
        try { latch.await(3, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        return result;
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
                if (name.isEmpty()) { Toast.makeText(requireContext(), "Enter a name", Toast.LENGTH_SHORT).show(); return; }
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
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addVideoToPlaylist(AppDatabase db, long playlistId, String playlistName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                PlaylistVideoEntity pv = new PlaylistVideoEntity();
                pv.playlistId = playlistId;
                pv.videoPath  = video.getPath() != null ? video.getPath() : "";
                pv.addedAt    = System.currentTimeMillis();
                pv.sortOrder  = (int)(System.currentTimeMillis() / 1000);
                db.playlistDao().addVideoToPlaylist(pv);
                // Update cover if not set
                PlaylistEntity playlist = db.playlistDao().getPlaylistById(playlistId);
                if (playlist != null && (playlist.coverVideoPath == null || playlist.coverVideoPath.isEmpty())) {
                    playlist.coverVideoPath = video.getPath();
                    playlist.updatedAt = System.currentTimeMillis();
                    db.playlistDao().updatePlaylist(playlist);
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

    // ── Share ─────────────────────────────────────────────────────────────────

    private void shareVideo() {
        try {
            File file = new File(video.getPath());
            if (!file.exists()) { Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show(); return; }
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

    // ── Favorite ──────────────────────────────────────────────────────────────

    private void toggleFavorite() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                boolean isFav = db.favoriteDao().isFavorite(video.getPath());
                if (isFav) {
                    db.favoriteDao().deleteByPath(video.getPath());
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show());
                } else {
                    FavoriteEntity fav = new FavoriteEntity();
                    fav.videoPath  = video.getPath();
                    fav.videoTitle = video.getTitle();
                    fav.videoDuration = video.getDuration();
                    fav.videoSize  = video.getSize();
                    fav.addedAt    = System.currentTimeMillis();
                    db.favoriteDao().insert(fav);
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Added to favorites ♥", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Error updating favorites", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ── Rename ────────────────────────────────────────────────────────────────

    private void showRenameDialog() {
        try {
            EditText et = new EditText(requireContext());
            String name = video.getTitle();
            et.setText(name);
            et.setSelection(name.length());
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename")
                .setView(et)
                .setPositiveButton("Rename", (d, w) -> {
                    String newName = et.getText() != null ? et.getText().toString().trim() : "";
                    if (newName.isEmpty()) return;
                    File src = new File(video.getPath());
                    File dst = new File(src.getParent(), newName + getExtension(src.getName()));
                    if (src.renameTo(dst)) {
                        Toast.makeText(requireContext(), "Renamed", Toast.LENGTH_SHORT).show();
                        // Trigger media scan
                        android.media.MediaScannerConnection.scanFile(requireContext(),
                            new String[]{dst.getAbsolutePath()}, null, null);
                    } else {
                        Toast.makeText(requireContext(), "Rename failed", Toast.LENGTH_SHORT).show();
                    }
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void showDeleteDialog() {
        try {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Video?")
                .setMessage(video.getTitle() + "\n\nThis will permanently delete the file.")
                .setPositiveButton("Delete", (d, w) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performDelete() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+
                List<Uri> uris = new ArrayList<>();
                if (video.getContentUri() != null && !video.getContentUri().isEmpty()) {
                    uris.add(Uri.parse(video.getContentUri()));
                } else {
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
                }
                if (!uris.isEmpty()) {
                    android.app.PendingIntent pi = MediaStore.createDeleteRequest(
                        requireContext().getContentResolver(), uris);
                    startIntentSenderForResult(pi.getIntentSender(), DELETE_REQUEST, null, 0, 0, 0, null);
                } else {
                    deleteDirectly();
                }
            } else {
                deleteDirectly();
            }
        } catch (IntentSender.SendIntentException e) {
            deleteDirectly();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteDirectly() {
        try {
            File file = new File(video.getPath());
            boolean deleted = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver cr = requireContext().getContentResolver();
                int rows = cr.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.DATA + "=?", new String[]{video.getPath()});
                deleted = rows > 0;
            }
            if (!deleted && file.exists()) deleted = file.delete();
            String msg = deleted ? "Deleted" : "Delete failed";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Download (copy to Downloads folder) ──────────────────────────────────

    private void downloadVideo() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File src = new File(video.getPath());
                if (!src.exists()) { showDlToast("File not found"); return; }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues vals = new ContentValues();
                    vals.put(MediaStore.Downloads.DISPLAY_NAME, src.getName());
                    vals.put(MediaStore.Downloads.MIME_TYPE, "video/mp4");
                    vals.put(MediaStore.Downloads.IS_PENDING, 1);
                    ContentResolver cr = requireContext().getContentResolver();
                    Uri col = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    Uri itemUri = cr.insert(col, vals);
                    if (itemUri == null) { showDlToast("Copy failed"); return; }
                    try (java.io.FileInputStream in = new java.io.FileInputStream(src);
                         java.io.OutputStream out = cr.openOutputStream(itemUri)) {
                        if (out == null) { showDlToast("Copy failed"); return; }
                        byte[] buf = new byte[65536]; int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                    vals.clear(); vals.put(MediaStore.Downloads.IS_PENDING, 0);
                    cr.update(itemUri, vals, null, null);
                } else {
                    java.io.File destDir = android.os.Environment
                        .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    if (destDir != null && !destDir.exists()) destDir.mkdirs();
                    java.io.File dest = new java.io.File(destDir, src.getName());
                    try (java.io.FileInputStream in = new java.io.FileInputStream(src);
                         java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                        byte[] buf = new byte[65536]; int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                }
                showDlToast("✅ Saved to Downloads: " + src.getName());
            } catch (Exception e) {
                showDlToast("Failed: " + e.getMessage());
            }
        });
    }

    private void showDlToast(String msg) {
        if (getActivity() != null)
            getActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show());
    }
}
