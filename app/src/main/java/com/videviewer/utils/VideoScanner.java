package com.videviewer.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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
            Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.MIME_TYPE
            };
            String selection = MediaStore.Video.Media.SIZE + " > 0";
            String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

            try (Cursor cursor = context.getContentResolver().query(
                    collection, projection, selection, null, sortOrder)) {
                if (cursor == null) return videos;

                while (cursor.moveToNext()) {
                    try {
                        VideoItem item = new VideoItem();
                        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
                        item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)));
                        item.setDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)));
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        item.setPath(path);
                        if (path != null) {
                            int lastSlash = path.lastIndexOf('/');
                            if (lastSlash > 0) item.setFolderPath(path.substring(0, lastSlash));
                        }
                        String folderName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
                        item.setFolderName(folderName != null ? folderName : "Unknown");
                        item.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)));
                        item.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
                        item.setDateAdded(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)));
                        item.setWidth(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)));
                        item.setHeight(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)));
                        item.setMimeType(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)));
                        if (item.getTitle() == null || item.getTitle().isEmpty()) {
                            item.setTitle(item.getDisplayName() != null ?
                                item.getDisplayName().replaceFirst("[.][^.]+$", "") : "Unknown");
                        }
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
            if (fp == null) continue;
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
        sorted.sort((a, b) -> Long.compare(b.getDateAdded(), a.getDateAdded()));
        return sorted;
    }
}
