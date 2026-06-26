package com.videviewer.fragments;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videviewer.R;
import com.videviewer.activities.VideoDetailsActivity;
import com.videviewer.database.AppDatabase;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VaultManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;

public class VideoOptionsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_VIDEO = "arg_video";
    private VideoItem video;
    private AppDatabase db;

    public static VideoOptionsBottomSheet newInstance(VideoItem video) {
        VideoOptionsBottomSheet sheet = new VideoOptionsBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable(ARG_VIDEO, video);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) video = getArguments().getParcelable(ARG_VIDEO);
        db = AppDatabase.getInstance(requireContext());
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

        ((TextView) view.findViewById(R.id.tv_video_title)).setText(video.getTitle());

        view.findViewById(R.id.option_details).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), VideoDetailsActivity.class);
            intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
            startActivity(intent);
            dismiss();
        });

        view.findViewById(R.id.option_share).setOnClickListener(v -> { shareVideo(); dismiss(); });

        // Download — ব্রাউজারের মতো ডাউনলোড করে
        view.findViewById(R.id.option_download).setOnClickListener(v -> { downloadVideo(); dismiss(); });

        view.findViewById(R.id.option_favorite).setOnClickListener(v -> { toggleFavorite(); dismiss(); });
        view.findViewById(R.id.option_rename).setOnClickListener(v -> showRenameDialog());
        view.findViewById(R.id.option_delete).setOnClickListener(v -> showDeleteConfirmation());
        view.findViewById(R.id.option_vault).setOnClickListener(v -> { moveToVault(); dismiss(); });
        view.findViewById(R.id.option_playlist).setOnClickListener(v -> {
            Toast.makeText(requireContext(), R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private void downloadVideo() {
        String path = video.getPath();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            downloadFromUrl(path);
        } else {
            copyLocalToDownloads(path);
        }
    }

    private void downloadFromUrl(String url) {
        try {
            String fileName = URLUtil.guessFileName(url, null, "video/*");
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setMimeType("video/*");
            req.setDescription(getString(R.string.downloading_file));
            req.setTitle(fileName);
            req.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager dm = (DownloadManager) requireContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(req);
                Toast.makeText(requireContext(),
                    getString(R.string.download_started) + ": " + fileName,
                    Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyLocalToDownloads(String filePath) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File src = new File(filePath);
                if (!src.exists()) { showToastOnUi(R.string.download_failed); return; }
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, src.getName());
                values.put(MediaStore.Downloads.MIME_TYPE, "video/*");
                values.put(MediaStore.Downloads.IS_PENDING, 1);
                ContentResolver resolver = requireContext().getContentResolver();
                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri item = resolver.insert(collection, values);
                if (item == null) { showToastOnUi(R.string.download_failed); return; }
                try (FileInputStream in = new FileInputStream(src);
                     OutputStream out = resolver.openOutputStream(item)) {
                    if (out == null) { showToastOnUi(R.string.download_failed); return; }
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                }
                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(item, values, null, null);
                showToastOnUi(R.string.download_completed);
            } catch (Exception e) {
                showToastOnUi(R.string.download_failed);
            }
        });
    }

    private void showToastOnUi(int resId) {
        if (getActivity() != null)
            getActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show());
    }

    private void shareVideo() {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                new File(video.getPath()));
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("video/*");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, getString(R.string.share_video)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.error_sharing, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean isFav = db.favoriteDao().isFavorite(video.getPath());
            if (isFav) db.favoriteDao().deleteByPath(video.getPath());
            else {
                com.videviewer.database.FavoriteEntity e = new com.videviewer.database.FavoriteEntity();
                e.videoPath = video.getPath(); e.videoTitle = video.getTitle();
                e.addedAt = System.currentTimeMillis();
                e.videoDuration = video.getDuration(); e.videoSize = video.getSize();
                db.favoriteDao().insert(e);
            }
            if (getActivity() != null)
                getActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(),
                        isFav ? R.string.removed_from_favorites : R.string.added_to_favorites,
                        Toast.LENGTH_SHORT).show());
        });
    }

    private void showRenameDialog() {
        EditText et = new EditText(requireContext());
        et.setText(video.getTitle()); et.selectAll();
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_video).setView(et)
            .setPositiveButton(R.string.rename, (d, w) -> {
                String n = et.getText().toString().trim();
                if (!n.isEmpty()) renameVideo(n); dismiss();
            })
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void renameVideo(String newName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Video.Media.DISPLAY_NAME, newName);
                int rows = requireContext().getContentResolver().update(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v,
                    MediaStore.Video.Media.DATA + "=?", new String[]{video.getPath()});
                boolean ok = rows > 0;
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            ok ? R.string.renamed_successfully : R.string.rename_failed,
                            Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.rename_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_video).setMessage(R.string.delete_video_confirm)
            .setPositiveButton(R.string.delete, (d, w) -> { deleteVideo(); dismiss(); })
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void deleteVideo() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int deleted = requireContext().getContentResolver().delete(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Media.DATA + "=?", new String[]{video.getPath()});
                boolean ok = deleted > 0;
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            ok ? R.string.deleted_successfully : R.string.delete_failed,
                            Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void moveToVault() {
        Executors.newSingleThreadExecutor().execute(() -> {
            String vaultPath = VaultManager.getInstance(requireContext()).moveToVault(video.getPath());
            if (vaultPath != null) {
                com.videviewer.database.VaultVideoEntity e = new com.videviewer.database.VaultVideoEntity();
                e.originalPath = video.getPath(); e.vaultPath = vaultPath;
                e.videoTitle = video.getTitle(); e.fileSize = video.getSize();
                e.duration = video.getDuration(); e.addedToVault = System.currentTimeMillis();
                e.originalFolder = video.getFolderName();
                db.vaultDao().insert(e);
            }
            boolean ok = vaultPath != null;
            if (getActivity() != null)
                getActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(),
                        ok ? R.string.moved_to_vault : R.string.vault_move_failed,
                        Toast.LENGTH_SHORT).show());
        });
    }
}
