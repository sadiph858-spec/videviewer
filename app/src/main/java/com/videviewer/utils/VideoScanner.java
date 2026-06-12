package com.videviewer.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import com.videviewer.models.VideoItem;
import java.util.ArrayList;
import java.util.List;

public class VideoScanner {

    private final Context context;

    public VideoScanner(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<VideoItem> scanAllVideos() {
        List<VideoItem> videos = new ArrayList<>();
        try {
            Uri collection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }

            String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.MIME_TYPE
            };

            String selection = MediaStore.Video.Media.SIZE + " > 0 AND "
                + MediaStore.Video.Media.DURATION + " > 0";
            String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

            try (Cursor cursor = context.getContentResolver().query(
                    collection, projection, selection, null, sortOrder)) {
                if (cursor == null) return videos;

                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
                int bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
                int dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
                int widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
                int heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);
                int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);

                while (cursor.moveToNext()) {
                    try {
                        VideoItem item = new VideoItem();
                        long id = cursor.getLong(idCol);
                        item.setId(id);

                        // Build a content URI for reliable playback on all API levels
                        Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id));
                        item.setContentUri(contentUri.toString());

                        // DATA column gives raw file path (available below Q, deprecated on Q+)
                        String path = (dataCol >= 0) ? cursor.getString(dataCol) : null;
                        item.setPath(path != null ? path : contentUri.toString());

                        if (path != null) {
                            int lastSlash = path.lastIndexOf('/');
                            if (lastSlash > 0) item.setFolderPath(path.substring(0, lastSlash));
                        }

                        String displayName = cursor.getString(nameCol);
                        item.setDisplayName(displayName);

                        String title = cursor.getString(titleCol);
                        if (title == null || title.isEmpty()) {
                            title = (displayName != null)
                                ? displayName.replaceFirst("[.][^.]+$", "") : "Unknown";
                        }
                        item.setTitle(title);

                        String bucketName = cursor.getString(bucketCol);
                        item.setFolderName(bucketName != null ? bucketName : "Unknown");

                        item.setSize(cursor.getLong(sizeCol));
                        item.setDuration(cursor.getLong(durCol));
                        item.setDateAdded(cursor.getLong(dateCol));
                        item.setDateModified(cursor.getLong(dateModCol));
                        item.setWidth(cursor.getInt(widthCol));
                        item.setHeight(cursor.getInt(heightCol));
                        item.setMimeType(cursor.getString(mimeCol));
                        item.setResolution(item.getWidth() + "x" + item.getHeight());

                        videos.add(item);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }

    public List<VideoItem> searchVideos(List<VideoItem> videos, String query) {
        List<VideoItem> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return videos;
        String lower = query.toLowerCase().trim();
        for (VideoItem v : videos) {
            if (v.getTitle() != null && v.getTitle().toLowerCase().contains(lower)) {
                results.add(v);
            }
        }
        return results;
    }

    public List<com.videviewer.models.FolderItem> getFolders(List<VideoItem> videos) {
        java.util.Map<String, com.videviewer.models.FolderItem> map = new java.util.LinkedHashMap<>();
        for (VideoItem video : videos) {
            String fp = video.getFolderPath();
            if (fp == null) fp = "Unknown";
            com.videviewer.models.FolderItem folder = map.get(fp);
            if (folder == null) {
                folder = new com.videviewer.models.FolderItem(fp, video.getFolderName());
                map.put(fp, folder);
            }
            folder.setVideoCount(folder.getVideoCount() + 1);
            folder.setTotalSize(folder.getTotalSize() + video.getSize());
            if (folder.getCoverVideoPath() == null) folder.setCoverVideoPath(video.getPath());
        }
        return new ArrayList<>(map.values());
    }

    public List<VideoItem> sortVideos(List<VideoItem> videos, String sortOrder) {
        List<VideoItem> sorted = new ArrayList<>(videos);
        if (sortOrder == null) sortOrder = AppConstants.SORT_DATE_NEW;
        switch (sortOrder) {
            case AppConstants.SORT_NAME_ASC:
                sorted.sort((a, b) -> a.getTitle() != null
                    ? a.getTitle().compareToIgnoreCase(b.getTitle() != null ? b.getTitle() : "") : 0);
                break;
            case AppConstants.SORT_NAME_DESC:
                sorted.sort((a, b) -> b.getTitle() != null
                    ? b.getTitle().compareToIgnoreCase(a.getTitle() != null ? a.getTitle() : "") : 0);
                break;
            case AppConstants.SORT_SIZE_LARGE:
                sorted.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));
                break;
            case AppConstants.SORT_SIZE_SMALL:
                sorted.sort((a, b) -> Long.compare(a.getSize(), b.getSize()));
                break;
            case AppConstants.SORT_DURATION_LONG:
                sorted.sort((a, b) -> Long.compare(b.getDuration(), a.getDuration()));
                break;
            case AppConstants.SORT_DURATION_SHORT:
                sorted.sort((a, b) -> Long.compare(a.getDuration(), b.getDuration()));
                break;
            case AppConstants.SORT_DATE_OLD:
                sorted.sort((a, b) -> Long.compare(a.getDateAdded(), b.getDateAdded()));
                break;
            default: // SORT_DATE_NEW
                sorted.sort((a, b) -> Long.compare(b.getDateAdded(), a.getDateAdded()));
        }
        return sorted;
    }
}
