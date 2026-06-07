package com.videviewer.models;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

/**
 * Playlist model for grouping videos
 */
public class Playlist implements Parcelable {

    private long id;
    private String name;
    private String description;
    private long createdAt;
    private long updatedAt;
    private List<String> videoPaths; // ordered list of video paths
    private String coverImagePath; // first video's thumbnail

    public Playlist() {
        videoPaths = new ArrayList<>();
        createdAt = System.currentTimeMillis();
        updatedAt = System.currentTimeMillis();
    }

    public Playlist(String name) {
        this();
        this.name = name;
    }

    protected Playlist(Parcel in) {
        id = in.readLong();
        name = in.readString();
        description = in.readString();
        createdAt = in.readLong();
        updatedAt = in.readLong();
        videoPaths = in.createStringArrayList();
        coverImagePath = in.readString();
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel in) { return new Playlist(in); }
        @Override
        public Playlist[] newArray(int size) { return new Playlist[size]; }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeLong(createdAt);
        dest.writeLong(updatedAt);
        dest.writeStringList(videoPaths);
        dest.writeString(coverImagePath);
    }

    @Override
    public int describeContents() { return 0; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getVideoPaths() { return videoPaths; }
    public void setVideoPaths(List<String> videoPaths) { this.videoPaths = videoPaths; }
    public String getCoverImagePath() { return coverImagePath; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }

    public int getVideoCount() { return videoPaths != null ? videoPaths.size() : 0; }

    public void addVideo(String path) {
        if (videoPaths == null) videoPaths = new ArrayList<>();
        if (!videoPaths.contains(path)) {
            videoPaths.add(path);
            updatedAt = System.currentTimeMillis();
        }
    }

    public void removeVideo(String path) {
        if (videoPaths != null) {
            videoPaths.remove(path);
            updatedAt = System.currentTimeMillis();
        }
    }
}
