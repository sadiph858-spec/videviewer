package com.videviewer.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * VideoItem - Data model for a video file
 * Implements Parcelable for passing between activities/fragments
 */
public class VideoItem implements Parcelable {

    private long id;
    private String title;
    private String displayName;
    private String path;
    private String folderPath;
    private String folderName;
    private long size;
    private long duration; // milliseconds
    private long dateAdded; // seconds since epoch
    private long dateModified;
    private int width;
    private int height;
    private String mimeType;
    private String resolution;
    private boolean isFavorite;
    private boolean isInVault;
    private int watchCount;
    private long lastWatched;
    private long resumePosition; // for resume playback
    private String thumbnailPath;

    public VideoItem() {}

    protected VideoItem(Parcel in) {
        id = in.readLong();
        title = in.readString();
        displayName = in.readString();
        path = in.readString();
        folderPath = in.readString();
        folderName = in.readString();
        size = in.readLong();
        duration = in.readLong();
        dateAdded = in.readLong();
        dateModified = in.readLong();
        width = in.readInt();
        height = in.readInt();
        mimeType = in.readString();
        resolution = in.readString();
        isFavorite = in.readByte() != 0;
        isInVault = in.readByte() != 0;
        watchCount = in.readInt();
        lastWatched = in.readLong();
        resumePosition = in.readLong();
        thumbnailPath = in.readString();
    }

    public static final Creator<VideoItem> CREATOR = new Creator<VideoItem>() {
        @Override
        public VideoItem createFromParcel(Parcel in) {
            return new VideoItem(in);
        }

        @Override
        public VideoItem[] newArray(int size) {
            return new VideoItem[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(displayName);
        dest.writeString(path);
        dest.writeString(folderPath);
        dest.writeString(folderName);
        dest.writeLong(size);
        dest.writeLong(duration);
        dest.writeLong(dateAdded);
        dest.writeLong(dateModified);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeString(mimeType);
        dest.writeString(resolution);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeByte((byte) (isInVault ? 1 : 0));
        dest.writeInt(watchCount);
        dest.writeLong(lastWatched);
        dest.writeLong(resumePosition);
        dest.writeString(thumbnailPath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

    public long getDateModified() { return dateModified; }
    public void setDateModified(long dateModified) { this.dateModified = dateModified; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public boolean isInVault() { return isInVault; }
    public void setInVault(boolean inVault) { isInVault = inVault; }

    public int getWatchCount() { return watchCount; }
    public void setWatchCount(int watchCount) { this.watchCount = watchCount; }

    public long getLastWatched() { return lastWatched; }
    public void setLastWatched(long lastWatched) { this.lastWatched = lastWatched; }

    public long getResumePosition() { return resumePosition; }
    public void setResumePosition(long resumePosition) { this.resumePosition = resumePosition; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    /**
     * Returns human-readable file size string
     */
    public String getFormattedSize() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Returns duration in HH:MM:SS or MM:SS format
     */
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%02d:%02d", minutes, secs);
    }

    @Override
    public String toString() {
        return "VideoItem{id=" + id + ", title='" + title + "', path='" + path + "'}";
    }
}
