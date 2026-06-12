package com.videviewer.fragments;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    // Android 11+ delete
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;

    // Android Q (10) delete via RecoverableSecurityException
    private ActivityResultLauncher<IntentSenderRequest> recoverableDeleteLauncher;

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

        // Android 11+ delete launcher
        deleteRequestLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                try {
                    String msg = result.getResultCode() == Activity.RESULT_OK
                        ? getString(R.string.deleted_successfully)
                        : getString(R.string.delete_failed);
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                } catch (Exception e) { e.printStackTrace(); }
                dismiss();
            });

        // Android Q recoverable delete launcher
        recoverableDeleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // User approved — retry delete
                    deleteViaContentResolverAfterApproval();
                } else {
                    try {
                        Toast.makeText(requireContext(),
                            R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) { e.printStackTrace(); }
                    dismiss();
                }
            });
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
                Toast.makeText(requireContext(), R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
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

    // ── Share ─────────────────────────────────────────────────────────────────
    private void shareVideo() {
        try {
            String path = video.getPath();
            Uri uri;
            if (path != null && (path.startsWith("content://") || path.startsWith("file://"))) {
                uri = Uri.parse(path);
            } else if (path != null) {
                File file = new File(path);
                if (!file.exists()) {
                    Toast.makeText(requireContext(), R.string.error_sharing, Toast.LENGTH_SHORT).show();
                    return;
                }
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
            try { Toast.makeText(requireContext(), R.string.error_sharing, Toast.LENGTH_SHORT).show(); }
            catch (Exception ignored) {}
        }
    }

    // ── Favorite ─────────────────────────────────────────────────────────────
    private void toggleFavorite() {
        if (video.getPath() == null) return;
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
                final boolean wasFav = isFav;
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            Toast.makeText(requireContext(),
                                wasFav ? R.string.removed_from_favorites : R.string.added_to_favorites,
                                Toast.LENGTH_SHORT).show();
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // ── Rename ────────────────────────────────────────────────────────────────
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void renameVideo(String newName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, newName);
                values.put(MediaStore.Video.Media.TITLE, newName);

                ContentResolver cr = requireContext().getContentResolver();
                int rows = 0;

                // Prefer content URI
                if (video.getContentUri() != null && !video.getContentUri().isEmpty()) {
                    try {
                        rows = cr.update(Uri.parse(video.getContentUri()), values, null, null);
                    } catch (Exception e) { e.printStackTrace(); }
                }

                // Fallback: DATA column
                if (rows == 0 && video.getPath() != null && !video.getPath().startsWith("content://")) {
                    try {
                        rows = cr.update(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values,
                            MediaStore.Video.Media.DATA + "=?",
                            new String[]{video.getPath()});
                    } catch (Exception e) { e.printStackTrace(); }
                }

                final int finalRows = rows;
                if (isAdded() && getActivity() != null) {
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
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try { Toast.makeText(requireContext(), R.string.rename_failed, Toast.LENGTH_SHORT).show(); }
                        catch (Exception ignored) {}
                    });
                }
            }
        });
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    private void showDeleteDialog() {
        try {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_video)
                .setMessage(R.string.delete_video_confirm)
                .setPositiveButton(R.string.delete, (d, w) -> deleteVideo())
                .setNegativeButton(R.string.cancel, null)
                .show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void deleteVideo() {
        try {
            Uri uri = resolveMediaUri();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: system dialog
                if (uri == null) {
                    showToast(R.string.delete_failed);
                    return;
                }
                List<Uri> uris = new ArrayList<>();
                uris.add(uri);
                PendingIntent pi = MediaStore.createDeleteRequest(
                    requireContext().getContentResolver(), uris);
                IntentSenderRequest req = new IntentSenderRequest.Builder(
                    pi.getIntentSender()).build();
                deleteRequestLauncher.launch(req);

            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // Android 10: try directly; catch RecoverableSecurityException
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        ContentResolver cr = requireContext().getContentResolver();
                        int deleted = 0;
                        if (uri != null) {
                            deleted = cr.delete(uri, null, null);
                        }
                        final int del = deleted;
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    showToast(del > 0 ? R.string.deleted_successfully : R.string.delete_failed);
                                    if (del > 0) dismiss();
                                } catch (Exception ignored) {}
                            });
                        }
                    } catch (android.app.RecoverableSecurityException rse) {
                        // Request user permission then retry
                        try {
                            IntentSenderRequest req = new IntentSenderRequest.Builder(
                                rse.getUserAction().getActionIntent().getIntentSender()).build();
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try { recoverableDeleteLauncher.launch(req); }
                                    catch (Exception e) { e.printStackTrace(); }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    try { showToast(R.string.delete_failed); dismiss(); }
                                    catch (Exception ignored) {}
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try { showToast(R.string.delete_failed); dismiss(); }
                                catch (Exception ignored) {}
                            });
                        }
                    }
                });

            } else {
                // Android 9 and below: direct delete
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        int deleted = 0;
                        ContentResolver cr = requireContext().getContentResolver();
                        if (uri != null) {
                            deleted = cr.delete(uri, null, null);
                        }
                        // Also delete the physical file if CR delete returned 0
                        if (deleted == 0 && video.getPath() != null) {
                            File f = new File(video.getPath());
                            if (f.exists() && f.delete()) deleted = 1;
                        }
                        final int del = deleted;
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    showToast(del > 0 ? R.string.deleted_successfully : R.string.delete_failed);
                                    dismiss();
                                } catch (Exception ignored) {}
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try { showToast(R.string.delete_failed); dismiss(); }
                                catch (Exception ignored) {}
                            });
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast(R.string.delete_failed);
        }
    }

    /** Called after user grants RecoverableSecurityException permission on Android Q */
    private void deleteViaContentResolverAfterApproval() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Uri uri = resolveMediaUri();
                int deleted = 0;
                if (uri != null) {
                    deleted = requireContext().getContentResolver().delete(uri, null, null);
                }
                final int del = deleted;
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            showToast(del > 0 ? R.string.deleted_successfully : R.string.delete_failed);
                            dismiss();
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try { showToast(R.string.delete_failed); dismiss(); }
                        catch (Exception ignored) {}
                    });
                }
            }
        });
    }

    /** Resolves the best content:// URI for this video */
    private Uri resolveMediaUri() {
        try {
            // Prefer stored content URI
            if (video.getContentUri() != null && video.getContentUri().startsWith("content://")) {
                return Uri.parse(video.getContentUri());
            }
            // Look up via DATA column
            if (video.getPath() != null && !video.getPath().startsWith("content://")) {
                try (Cursor cursor = requireContext().getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Video.Media._ID},
                        MediaStore.Video.Media.DATA + "=?",
                        new String[]{video.getPath()}, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        long id = cursor.getLong(
                            cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                        return Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    }
                }
            }
            // Last fallback: path is already a content URI
            if (video.getPath() != null && video.getPath().startsWith("content://")) {
                return Uri.parse(video.getPath());
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private void showToast(int resId) {
        try { Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show(); }
        catch (Exception ignored) {}
    }
}
