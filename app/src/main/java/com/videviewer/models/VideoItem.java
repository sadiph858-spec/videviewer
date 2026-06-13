package com.videviewer.models;

  public class VideoItem {
      public long id;
      public String title;
      public String path;
      public String folder;
      public long duration;
      public long size;
      public String mimeType;
      public long dateAdded;
      public boolean isFavorite;

      public VideoItem() {}

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
          int dot = title.lastIndexOf('.');
          return dot >= 0 ? title.substring(dot + 1).toUpperCase() : "MP4";
      }
  }