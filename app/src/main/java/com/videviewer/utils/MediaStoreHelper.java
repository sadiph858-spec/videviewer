package com.videviewer.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.videviewer.models.VideoItem;
import java.util.ArrayList;
import java.util.List;

public class MediaStoreHelper {

    public static List<VideoItem> getAllVideos(Context context) {
        List<VideoItem> videos = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED
        };
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idCol     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int titleCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                int dataCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int folderCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                int durCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int mimeCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
                int dateCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
                do {
                    VideoItem item = new VideoItem();
                    item.setId(cursor.getLong(idCol));
                    item.setTitle(cursor.getString(titleCol));
                    item.setPath(cursor.getString(dataCol));
                    item.setFolderName(cursor.getString(folderCol));
                    item.setDuration(cursor.getLong(durCol));
                    item.setSize(cursor.getLong(sizeCol));
                    item.setMimeType(cursor.getString(mimeCol));
                    item.setDateAdded(cursor.getLong(dateCol));
                    item.setFolderPath(cursor.getString(dataCol));
                    if (item.getPath() != null && new java.io.File(item.getPath()).exists()) {
                        videos.add(item);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }

    public static boolean deleteVideo(Context context, String path) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        int deleted = context.getContentResolver().delete(
            uri, MediaStore.Video.Media.DATA + "=?", new String[]{path});
        return deleted > 0;
    }
}
