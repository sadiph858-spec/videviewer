package com.videviewer.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import com.videviewer.models.FolderItem;
import com.videviewer.models.VideoItem;
import java.util.*;

/**
 * VideoScanner - Scans device storage using MediaStore for all videos
 * Uses Scoped Storage compatible API (Android 10+)
 */
public class VideoScanner {

    private static final String TAG = "VideoScanner";

    private final Context context;

    public VideoScanner(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Scan all videos from MediaStore
     */
    public List<VideoItem> scanAllVideos() {
        List<VideoItem> videos = new ArrayList<>();

        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

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
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.RELATIVE_PATH
        };

        String selection = MediaStore.Video.Media.SIZE + " > 0"
            + " AND " + MediaStore.Video.Media.DURATION + " > 0";
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                collection, projection, selection, null, sortOrder)) {

            if (cursor == null) return videos;

            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            int displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            int dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
            int dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
            int widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);
            int heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);
            int mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);

            while (cursor.moveToNext()) {
                VideoItem item = new VideoItem();
                item.setId(cursor.getLong(idCol));
                item.setTitle(cursor.getString(titleCol));
                item.setDisplayName(cursor.getString(displayNameCol));
                String path = cursor.getString(dataCol);
                item.setPath(path);

                // Extract folder info
                if (path != null) {
                    int lastSlash = path.lastIndexOf('/');
                    if (lastSlash > 0) {
                        item.setFolderPath(path.substring(0, lastSlash));
                    }
                }

                String folderName = cursor.getString(bucketNameCol);
                item.setFolderName(folderName != null ? folderName : "Unknown");
                item.setSize(cursor.getLong(sizeCol));
                item.setDuration(cursor.getLong(durationCol));
                item.setDateAdded(cursor.getLong(dateAddedCol));
                item.setDateModified(cursor.getLong(dateModifiedCol));
                item.setWidth(cursor.getInt(widthCol));
                item.setHeight(cursor.getInt(heightCol));
                item.setMimeType(cursor.getString(mimeTypeCol));

                int w = item.getWidth();
                int h = item.getHeight();
                item.setResolution(w > 0 && h > 0 ? w + "x" + h : "Unknown");

                if (item.getTitle() == null || item.getTitle().isEmpty()) {
                    item.setTitle(item.getDisplayName() != null ?
                        item.getDisplayName().replaceFirst("[.][^.]+$", "") : "Unknown");
                }

                videos.add(item);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error scanning videos", e);
        }

        return videos;
    }

    /**
     * Group videos by folder
     */
    public List<FolderItem> getFolders(List<VideoItem> videos) {
        Map<String, FolderItem> folderMap = new LinkedHashMap<>();

        for (VideoItem video : videos) {
            String folderPath = video.getFolderPath();
            if (folderPath == null) continue;

            FolderItem folder = folderMap.get(folderPath);
            if (folder == null) {
                folder = new FolderItem(folderPath, video.getFolderName());
                folderMap.put(folderPath, folder);
            }

            folder.setVideoCount(folder.getVideoCount() + 1);
            folder.setTotalSize(folder.getTotalSize() + video.getSize());

            if (folder.getCoverVideoPath() == null) {
                folder.setCoverVideoPath(video.getPath());
            }
        }

        return new ArrayList<>(folderMap.values());
    }

    /**
     * Sort videos by given sort order
     */
    public List<VideoItem> sortVideos(List<VideoItem> videos, String sortOrder) {
        List<VideoItem> sorted = new ArrayList<>(videos);

        switch (sortOrder) {
            case AppConstants.SORT_NAME_ASC:
                sorted.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;
            case AppConstants.SORT_NAME_DESC:
                sorted.sort((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
                break;
            case AppConstants.SORT_DATE_NEW:
                sorted.sort((a, b) -> Long.compare(b.getDateAdded(), a.getDateAdded()));
                break;
            case AppConstants.SORT_DATE_OLD:
                sorted.sort((a, b) -> Long.compare(a.getDateAdded(), b.getDateAdded()));
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
            default:
                sorted.sort((a, b) -> Long.compare(b.getDateAdded(), a.getDateAdded()));
        }

        return sorted;
    }

    /**
     * Search videos by title
     */
    public List<VideoItem> searchVideos(List<VideoItem> videos, String query) {
        if (query == null || query.trim().isEmpty()) return videos;
        String lowerQuery = query.toLowerCase().trim();
        List<VideoItem> results = new ArrayList<>();
        for (VideoItem video : videos) {
            if (video.getTitle().toLowerCase().contains(lowerQuery)
                || (video.getFolderName() != null && video.getFolderName().toLowerCase().contains(lowerQuery))) {
                results.add(video);
            }
        }
        return results;
    }
}
