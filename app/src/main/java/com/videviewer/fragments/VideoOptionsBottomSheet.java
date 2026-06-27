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
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        view.findViewById(R.id.option_share).setOnClickListener(v -> {
            shareVideo(); dismiss();
        });

        view.findViewById(R.id.option_favorite).setOnClickListener(v -> {
            toggleFavorite(); dismiss();
        });

        view.findViewById(R.id.option_rename).setOnClickListener(v -> showRenameDialog());

        view.findViewById(R.id.option_delete).setOnClickListener(v -> {
            showDeleteDialog(); dismiss();
        });

        view.findViewById(R.id.option_vault).setOnClickListener(v -> {
            VaultActivity.moveToVault(requireContext(), video);
            Toast.makeText(requireContext(), "Moving to vault...", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        view.findViewById(R.id.option_download).setOnClickListener(v -> {
            downloadVideo(); dismiss();
        });


        view.findViewById(R.id.option_playlist).setOnClickListener(v -> {
            Toast.makeText(requireContext(), R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private void shareVideo() {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                new File(video.getPath()));
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Video"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Cannot share this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                boolean isFav = db.favoriteDao().isFavorite(video.getPath());
                if (isFav) {
                    db.favoriteDao().deleteByPath(video.getPath());
                } else {
                    FavoriteEntity e = new FavoriteEntity();
                    e.videoPath = video.getPath();
                    e.videoTitle = video.getTitle();
                    e.addedAt = System.currentTimeMillis();
                    e.videoDuration = video.getDuration();
                    e.videoSize = video.getSize();
                    db.favoriteDao().insert(e);
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            isFav ? "Removed from favorites" : "Added to favorites",
                            Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void showRenameDialog() {
        EditText et = new EditText(requireContext());
        et.setText(video.getTitle());
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Video")
            .setView(et)
            .setPositiveButton("Rename", (d, w) -> {
                String name = et.getText().toString().trim();
                if (!name.isEmpty()) renameVideo(name);
                dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void renameVideo(String newName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, newName);
                int rows = requireContext().getContentResolver().update(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values,
                    MediaStore.Video.Media.DATA + "=?",
                    new String[]{video.getPath()});
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            rows > 0 ? "Renamed successfully" : "Rename failed",
                            Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void showDeleteDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Video")
            .setMessage("Are you sure you want to delete this video?")
            .setPositiveButton("Delete", (d, w) -> deleteVideo())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteVideo() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - use createDeleteRequest
                ContentResolver resolver = requireContext().getContentResolver();
                Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                List<Uri> uris = new ArrayList<>();

                try (android.database.Cursor c = resolver.query(videoUri,
                        new String[]{MediaStore.Video.Media._ID},
                        MediaStore.Video.Media.DATA + "=?",
                        new String[]{video.getPath()}, null)) {
                    if (c != null && c.moveToFirst()) {
                        long id = c.getLong(0);
                        uris.add(Uri.withAppendedPath(videoUri, String.valueOf(id)));
                    }
                }

                if (!uris.isEmpty()) {
                    android.app.PendingIntent pi = MediaStore.createDeleteRequest(resolver, uris);
                    if (getActivity() != null) {
                        getActivity().startIntentSenderForResult(
                            pi.getIntentSender(), DELETE_REQUEST, null, 0, 0, 0);
                    }
                }
            } else {
                // Android 10 and below
                int deleted = requireContext().getContentResolver().delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.DATA + "=?",
                    new String[]{video.getPath()});
                Toast.makeText(requireContext(),
                    deleted > 0 ? "Deleted successfully" : "Delete failed",
                    Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadVideo() {
        String path = video.getPath();
        if (path == null) return;
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                String fileName = android.webkit.URLUtil.guessFileName(path, null, "video/*");
                android.app.DownloadManager.Request req =
                    new android.app.DownloadManager.Request(android.net.Uri.parse(path));
                req.setMimeType("video/*");
                req.setDescription("Downloading…");
                req.setTitle(fileName);
                req.setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
                android.app.DownloadManager dm =
                    (android.app.DownloadManager) requireContext()
                        .getSystemService(android.content.Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(req);
                    Toast.makeText(requireContext(), "Download started", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Download failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Local file — copy to Downloads via MediaStore
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    java.io.File src = new java.io.File(path);
                    if (!src.exists()) { showDlToast("File not found"); return; }
                    ContentValues vals = new ContentValues();
                    vals.put(MediaStore.Downloads.DISPLAY_NAME, src.getName());
                    vals.put(MediaStore.Downloads.MIME_TYPE, "video/mp4");
                    vals.put(MediaStore.Downloads.IS_PENDING, 1);
                    ContentResolver cr = requireContext().getContentResolver();
                    Uri col  = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    Uri item = cr.insert(col, vals);
                    if (item == null) { showDlToast("Download failed"); return; }
                    try (java.io.FileInputStream in  = new java.io.FileInputStream(src);
                         java.io.OutputStream    out = cr.openOutputStream(item)) {
                        if (out == null) { showDlToast("Download failed"); return; }
                        byte[] buf = new byte[8192]; int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                    vals.clear();
                    vals.put(MediaStore.Downloads.IS_PENDING, 0);
                    cr.update(item, vals, null, null);
                    showDlToast("Saved to Downloads folder");
                } catch (Exception e) { showDlToast("Download failed"); }
            });
        }
    }

    private void showDlToast(String msg) {
        if (getActivity() != null)
            getActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
    }

}