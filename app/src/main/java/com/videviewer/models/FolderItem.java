package com.videviewer.models;

public class FolderItem {

    private String folderPath;
    private String folderName;
    private int videoCount;
    private long totalSize;
    private long lastModified;
    private String coverVideoPath;

    public FolderItem() {}

    public FolderItem(String folderPath, String folderName) {
        this.folderPath = folderPath;
        this.folderName = folderName;
    }

    public String getFolderPath()  { return folderPath; }
    public void setFolderPath(String v) { this.folderPath = v; }

    public String getFolderName()  { return folderName; }
    public void setFolderName(String v) { this.folderName = v; }

    public int  getVideoCount()    { return videoCount; }
    public void setVideoCount(int v) { this.videoCount = v; }

    public long getTotalSize()     { return totalSize; }
    public void setTotalSize(long v) { this.totalSize = v; }

    public long getLastModified()  { return lastModified; }
    public void setLastModified(long v) { this.lastModified = v; }

    public String getCoverVideoPath()  { return coverVideoPath; }
    public void setCoverVideoPath(String v) { this.coverVideoPath = v; }

    /** Alias so StorageFragment can call getCoverPath() */
    public String getCoverPath()   { return coverVideoPath; }
    public void setCoverPath(String v)  { this.coverVideoPath = v; }

    public String getFormattedSize() {
        if (totalSize < 1024 * 1024) return String.format("%.1f KB", totalSize / 1024.0);
        if (totalSize < 1024L * 1024 * 1024) return String.format("%.1f MB", totalSize / (1024.0 * 1024));
        return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
    }

    public String getVideoCountText() {
        return videoCount + (videoCount == 1 ? " video" : " videos");
    }
}
