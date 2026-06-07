package com.videviewer.models;

/**
 * FolderItem - Represents a folder containing videos
 */
public class FolderItem {

    private String folderPath;
    private String folderName;
    private int videoCount;
    private long totalSize;
    private long lastModified;
    private String coverVideoPath; // for thumbnail

    public FolderItem() {}

    public FolderItem(String folderPath, String folderName) {
        this.folderPath = folderPath;
        this.folderName = folderName;
    }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public int getVideoCount() { return videoCount; }
    public void setVideoCount(int videoCount) { this.videoCount = videoCount; }

    public long getTotalSize() { return totalSize; }
    public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public String getCoverVideoPath() { return coverVideoPath; }
    public void setCoverVideoPath(String coverVideoPath) { this.coverVideoPath = coverVideoPath; }

    public String getFormattedSize() {
        if (totalSize < 1024 * 1024) return String.format("%.1f KB", totalSize / 1024.0);
        if (totalSize < 1024L * 1024 * 1024) return String.format("%.1f MB", totalSize / (1024.0 * 1024));
        return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
    }

    public String getVideoCountText() {
        return videoCount + (videoCount == 1 ? " video" : " videos");
    }
}
