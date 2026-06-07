package com.videviewer.fragments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.concurrent.Executors;

/**
 * VideoOptionsBottomSheet - Long-press context menu
 * Options: Play, Details, Favorite, Share, Rename, Delete, Add to Vault, Add to Playlist
 */
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
        if (getArguments() != null) {
            video = getArguments().getParcelable(ARG_VIDEO);
        }
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

        TextView tvTitle = view.findViewById(R.id.tv_video_title);
        tvTitle.setText(video.getTitle());

        // Details
        view.findViewById(R.id.option_details).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), VideoDetailsActivity.class);
            intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
            startActivity(intent);
            dismiss();
        });

        // Share
        view.findViewById(R.id.option_share).setOnClickListener(v -> {
            shareVideo();
            dismiss();
        });

        // Favorite toggle
        view.findViewById(R.id.option_favorite).setOnClickListener(v -> {
            toggleFavorite();
            dismiss();
        });

        // Rename
        view.findViewById(R.id.option_rename).setOnClickListener(v -> {
            showRenameDialog();
        });

        // Delete
        view.findViewById(R.id.option_delete).setOnClickListener(v -> {
            showDeleteConfirmation();
        });

        // Move to Vault
        view.findViewById(R.id.option_vault).setOnClickListener(v -> {
            moveToVault();
            dismiss();
        });

        // Add to Playlist
        view.findViewById(R.id.option_playlist).setOnClickListener(v -> {
            // TODO: Show playlist picker
            Toast.makeText(requireContext(), R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private void shareVideo() {
        try {
            Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                new File(video.getPath()));
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("video/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_video)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.error_sharing, Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean isFav = db.favoriteDao().isFavorite(video.getPath());
            if (isFav) {
                db.favoriteDao().deleteByPath(video.getPath());
            } else {
                com.videviewer.database.FavoriteEntity entity = new com.videviewer.database.FavoriteEntity();
                entity.videoPath = video.getPath();
                entity.videoTitle = video.getTitle();
                entity.addedAt = System.currentTimeMillis();
                entity.videoDuration = video.getDuration();
                entity.videoSize = video.getSize();
                db.favoriteDao().insert(entity);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(),
                        isFav ? R.string.removed_from_favorites : R.string.added_to_favorites,
                        Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showRenameDialog() {
        EditText editText = new EditText(requireContext());
        editText.setText(video.getTitle());
        editText.selectAll();

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rename_video)
            .setView(editText)
            .setPositiveButton(R.string.rename, (dialog, which) -> {
                String newName = editText.getText().toString().trim();
                if (!newName.isEmpty()) {
                    renameVideo(newName);
                }
                dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void renameVideo(String newName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ContentResolver resolver = requireContext().getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, newName);

                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                String where = MediaStore.Video.Media.DATA + "=?";
                int rows = resolver.update(uri, values, where, new String[]{video.getPath()});

                if (getActivity() != null) {
                    boolean success = rows > 0;
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            success ? R.string.renamed_successfully : R.string.rename_failed,
                            Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.rename_failed, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_video)
            .setMessage(R.string.delete_video_confirm)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                deleteVideo();
                dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void deleteVideo() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                String where = MediaStore.Video.Media.DATA + "=?";
                int deleted = requireContext().getContentResolver()
                    .delete(uri, where, new String[]{video.getPath()});

                if (getActivity() != null) {
                    boolean success = deleted > 0;
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            success ? R.string.deleted_successfully : R.string.delete_failed,
                            Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void moveToVault() {
        Executors.newSingleThreadExecutor().execute(() -> {
            String vaultPath = VaultManager.getInstance(requireContext())
                .moveToVault(video.getPath());

            if (vaultPath != null) {
                // Save to vault DB
                com.videviewer.database.VaultVideoEntity entity = new com.videviewer.database.VaultVideoEntity();
                entity.originalPath = video.getPath();
                entity.vaultPath = vaultPath;
                entity.videoTitle = video.getTitle();
                entity.fileSize = video.getSize();
                entity.duration = video.getDuration();
                entity.addedToVault = System.currentTimeMillis();
                entity.originalFolder = video.getFolderName();
                db.vaultDao().insert(entity);
            }

            if (getActivity() != null) {
                boolean success = vaultPath != null;
                getActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(),
                        success ? R.string.moved_to_vault : R.string.vault_move_failed,
                        Toast.LENGTH_SHORT).show());
            }
        });
    }
}
