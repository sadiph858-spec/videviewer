package com.videviewer.fragments;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class VideoOptionsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_VIDEO = "arg_video";
    private VideoItem video;

    // For Android 11+ delete
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;

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

        // Register delete launcher (Android 11+ requires user confirmation)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Toast.makeText(requireContext(),
                            R.string.deleted_successfully, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                            R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    }
                    dismiss();
                });
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
                Toast.makeText(requireContext(),
                    R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
                dismiss();
            });

            view.findViewById(R.id.option_playlist).setOnClickListener(v -> {
                Toast.makeText(requireContext(),
                    R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
                dismiss();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shareVideo() {
        try {
            String path = video.getPath();
            Uri uri;
            if (path != null && path.startsWith("content://")) {
                uri = Uri.parse(path);
            } else if (path != null) {
                File file = new File(path);
                uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);
            } else {
                Toast.makeText(requireContext(), R.string.error_sharing, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Video"));
        } catch (Exception e) {
            e.printStackTrace();
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
                    getActivity().runOnUiThread(() -> {
                        try {
                            Toast.makeText(requireContext(),
                                isFav ? R.string.removed_from_favorites : R.string.added_to_favorites,
                                Toast.LENGTH_SHORT).show();
                        } catch (Exception ignored) {}
                    });
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
                values.put(MediaStore.Video.Media.TITLE, newName);

                int rows = 0;
                ContentResolver cr = requireContext().getContentResolver();

                // On Android Q+, prefer content URI; fall back to DATA column
                if (video.getContentUri() != null && !video.getContentUri().isEmpty()) {
                    Uri contentUri = Uri.parse(video.getContentUri());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Must clear pending flag before updating
                        try {
                            values.put(MediaStore.Video.Media.IS_PENDING, 0);
                        } catch (Exception ignored) {}
                    }
                    rows = cr.update(contentUri, values, null, null);
                }

                // Fallback: use DATA column selection
                if (rows == 0 && video.getPath() != null && !video.getPath().startsWith("content://")) {
                    rows = cr.update(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values,
                        MediaStore.Video.Media.DATA + "=?",
                        new String[]{video.getPath()});
                }

                final int finalRows = rows;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            Toast.makeText(requireContext(),
                                finalRows > 0 ? R.string.renamed_successfully : R.string.rename_failed,
                                Toast.LENGTH_SHORT).show();
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            Toast.makeText(requireContext(),
                                R.string.rename_failed, Toast.LENGTH_SHORT).show();
                        } catch (Exception ignored) {}
                    });
                }
            }
        });
    }

    private void showDeleteDialog() {
        try {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_video)
                .setMessage(R.string.delete_video_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> deleteVideo())
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteVideo() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: use createDeleteRequest
                List<Uri> uris = new ArrayList<>();
                Uri uri = null;
                if (video.getContentUri() != null && !video.getContentUri().isEmpty()) {
                    uri = Uri.parse(video.getContentUri());
                } else if (video.getPath() != null) {
                    uri = getUriFromPath(video.getPath());
                }
                if (uri != null) {
                    uris.add(uri);
                    PendingIntent pi = MediaStore.createDeleteRequest(
                        requireContext().getContentResolver(), uris);
                    IntentSenderRequest request = new IntentSenderRequest.Builder(
                        pi.getIntentSender()).build();
                    if (deleteRequestLauncher != null) {
                        deleteRequestLauncher.launch(request);
                    }
                }
            } else {
                // Android 10 and below: direct deletion
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        int deleted = 0;
                        ContentResolver cr = requireContext().getContentResolver();

                        if (video.getContentUri() != null && !video.getContentUri().isEmpty()) {
                            deleted = cr.delete(Uri.parse(video.getContentUri()), null, null);
                        }
                        if (deleted == 0 && video.getPath() != null) {
                            deleted = cr.delete(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                MediaStore.Video.Media.DATA + "=?",
                                new String[]{video.getPath()});
                        }

                        final int del = deleted;
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    Toast.makeText(requireContext(),
                                        del > 0 ? R.string.deleted_successfully : R.string.delete_failed,
                                        Toast.LENGTH_SHORT).show();
                                    dismiss();
                                } catch (Exception ignored) {}
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    Toast.makeText(requireContext(),
                                        R.string.delete_failed, Toast.LENGTH_SHORT).show();
                                    dismiss();
                                } catch (Exception ignored) {}
                            });
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private Uri getUriFromPath(String path) {
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Media._ID},
                MediaStore.Video.Media.DATA + "=?",
                new String[]{path}, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    String.valueOf(id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
