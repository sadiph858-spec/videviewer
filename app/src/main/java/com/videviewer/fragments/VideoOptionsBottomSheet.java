package com.videviewer.fragments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
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
import com.videviewer.activities.VideoDetailsActivity;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.FavoriteEntity;
import com.videviewer.models.VideoItem;
import com.videviewer.utils.AppConstants;
import java.io.File;
import java.util.concurrent.Executors;

public class VideoOptionsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_VIDEO = "arg_video";
    private VideoItem video;

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

        try {
            TextView tvTitle = view.findViewById(R.id.tv_video_title);
            if (tvTitle != null) tvTitle.setText(video.getTitle());

            view.findViewById(R.id.option_details).setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(requireContext(), VideoDetailsActivity.class);
                    intent.putExtra(AppConstants.EXTRA_VIDEO_PATH, video.getPath());
                    startActivity(intent);
                    dismiss();
                } catch (Exception e) { e.printStackTrace(); }
            });

            view.findViewById(R.id.option_share).setOnClickListener(v -> {
                shareVideo(); dismiss();
            });

            view.findViewById(R.id.option_favorite).setOnClickListener(v -> {
                toggleFavorite(); dismiss();
            });

            view.findViewById(R.id.option_rename).setOnClickListener(v -> showRenameDialog());

            view.findViewById(R.id.option_delete).setOnClickListener(v -> showDeleteDialog());

            view.findViewById(R.id.option_vault).setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Moving to vault...", Toast.LENGTH_SHORT).show();
                dismiss();
            });

            view.findViewById(R.id.option_playlist).setOnClickListener(v -> {
                Toast.makeText(requireContext(), R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
                dismiss();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shareVideo() {
        try {
            File file = new File(video.getPath());
            Uri uri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Video"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.error_sharing, Toast.LENGTH_SHORT).show();
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
                    FavoriteEntity entity = new FavoriteEntity();
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showRenameDialog() {
        try {
            EditText et = new EditText(requireContext());
            et.setText(video.getTitle());
            et.selectAll();
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.rename_video)
                .setView(et)
                .setPositiveButton(R.string.rename, (d, w) -> {
                    String newName = et.getText().toString().trim();
                    if (!newName.isEmpty()) renameVideo(newName);
                    dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renameVideo(String newName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, newName);
                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                int rows = requireContext().getContentResolver()
                    .update(uri, values,
                        MediaStore.Video.Media.DATA + "=?",
                        new String[]{video.getPath()});
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            rows > 0 ? R.string.renamed_successfully : R.string.rename_failed,
                            Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            R.string.rename_failed, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void showDeleteDialog() {
        try {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_video)
                .setMessage(R.string.delete_video_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    deleteVideo(); dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteVideo() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                int deleted = requireContext().getContentResolver()
                    .delete(uri,
                        MediaStore.Video.Media.DATA + "=?",
                        new String[]{video.getPath()});
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            deleted > 0 ? R.string.deleted_successfully : R.string.delete_failed,
                            Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                            R.string.delete_failed, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
