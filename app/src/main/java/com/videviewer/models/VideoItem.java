package com.videviewer.models;

  import android.os.Parcel;
  import android.os.Parcelable;

  public class VideoItem implements Parcelable {
      // Public fields (backward compat with VideoAdapter)
      public long id;
      public String title;
      public String path;
      public String folder;
      public long duration;
      public long size;
      public String mimeType;
      public long dateAdded;
      public boolean isFavorite;

      // Extended fields used by VideoScanner
      private String contentUri;
      private String folderPath;
      private String displayName;
      private String folderName;
      private long dateModified;
      private int width;
      private int height;
      private String resolution;

      public VideoItem() {}

      // --- Parcelable ---
      protected VideoItem(Parcel in) {
          id = in.readLong();
          title = in.readString();
          path = in.readString();
          folder = in.readString();
          duration = in.readLong();
          size = in.readLong();
          mimeType = in.readString();
          dateAdded = in.readLong();
          isFavorite = in.readByte() != 0;
          contentUri = in.readString();
          folderPath = in.readString();
          displayName = in.readString();
          folderName = in.readString();
          dateModified = in.readLong();
          width = in.readInt();
          height = in.readInt();
          resolution = in.readString();
      }

      @Override
      public void writeToParcel(Parcel dest, int flags) {
          dest.writeLong(id);
          dest.writeString(title);
          dest.writeString(path);
          dest.writeString(folder);
          dest.writeLong(duration);
          dest.writeLong(size);
          dest.writeString(mimeType);
          dest.writeLong(dateAdded);
          dest.writeByte((byte) (isFavorite ? 1 : 0));
          dest.writeString(contentUri);
          dest.writeString(folderPath);
          dest.writeString(displayName);
          dest.writeString(folderName);
          dest.writeLong(dateModified);
          dest.writeInt(width);
          dest.writeInt(height);
          dest.writeString(resolution);
      }

      @Override public int describeContents() { return 0; }

      public static final Creator<VideoItem> CREATOR = new Creator<VideoItem>() {
          @Override public VideoItem createFromParcel(Parcel in) { return new VideoItem(in); }
          @Override public VideoItem[] newArray(int size) { return new VideoItem[size]; }
      };

      // --- Getters & Setters ---
      public long getId() { return id; }
      public void setId(long id) { this.id = id; }

      public String getTitle() { return title; }
      public void setTitle(String title) { this.title = title; }

      public String getPath() { return path; }
      public void setPath(String path) { this.path = path; }

      public String getFolder() { return folder; }
      public void setFolder(String folder) { this.folder = folder; }

      public long getDuration() { return duration; }
      public void setDuration(long duration) { this.duration = duration; }

      public long getSize() { return size; }
      public void setSize(long size) { this.size = size; }

      public String getMimeType() { return mimeType; }
      public void setMimeType(String mimeType) { this.mimeType = mimeType; }

      public long getDateAdded() { return dateAdded; }
      public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

      public boolean isFavorite() { return isFavorite; }
      public void setFavorite(boolean favorite) { this.isFavorite = favorite; }

      public String getContentUri() { return contentUri; }
      public void setContentUri(String contentUri) { this.contentUri = contentUri; }

      public String getFolderPath() { return folderPath; }
      public void setFolderPath(String folderPath) {
          this.folderPath = folderPath;
          this.folder = folderPath;
      }

      public String getDisplayName() { return displayName; }
      public void setDisplayName(String displayName) { this.displayName = displayName; }

      public String getFolderName() { return folderName; }
      public void setFolderName(String folderName) {
          this.folderName = folderName;
          this.folder = folderName;
      }

      public long getDateModified() { return dateModified; }
      public void setDateModified(long dateModified) { this.dateModified = dateModified; }

      public int getWidth() { return width; }
      public void setWidth(int width) { this.width = width; }

      public int getHeight() { return height; }
      public void setHeight(int height) { this.height = height; }

      public String getResolution() { return resolution; }
      public void setResolution(String resolution) { this.resolution = resolution; }

      /** Returns the best URI for playback (contentUri preferred over file path) */
      public String getPlaybackUri() {
          return (contentUri != null && !contentUri.isEmpty()) ? contentUri : path;
      }

      private long resumePosition;
      public long getResumePosition() { return resumePosition; }
      public void setResumePosition(long resumePosition) { this.resumePosition = resumePosition; }

      public String getFormattedDuration() {
          long s = duration / 1000;
          long h = s / 3600;
          long m = (s % 3600) / 60;
          long sec = s % 60;
          if (h > 0) return String.format("%d:%02d:%02d", h, m, sec);
          return String.format("%02d:%02d", m, sec);
      }

      public String getFormattedSize() {
          if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024f);
          if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024f * 1024));
          return String.format("%.2f GB", size / (1024f * 1024 * 1024));
      }

      public String getExtension() {
          String name = (title != null) ? title : "";
          int dot = name.lastIndexOf('.');
          return dot >= 0 ? name.substring(dot + 1).toUpperCase() : "MP4";
      }
  }
  