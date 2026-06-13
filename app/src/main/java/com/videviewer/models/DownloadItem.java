package com.videviewer.models;

  public class DownloadItem {
      public String url;
      public String filename;
      public int progress;
      public int status;
      public long totalBytes;
      public long downloadedBytes;
      public double speedMbps;

      public static final int STATUS_DOWNLOADING = 0;
      public static final int STATUS_PAUSED = 1;
      public static final int STATUS_COMPLETED = 2;
      public static final int STATUS_FAILED = 3;

      public DownloadItem(String url, int progress, int status) {
          this.url = url;
          this.progress = progress;
          this.status = status;
          int slash = url.lastIndexOf('/');
          this.filename = slash >= 0 ? url.substring(slash + 1) : url;
      }

      public String getStatusLabel() {
          switch (status) {
              case STATUS_DOWNLOADING: return String.format("%.1f MB/s · %d%%", speedMbps, progress);
              case STATUS_PAUSED: return "Paused";
              case STATUS_COMPLETED: return "Completed";
              case STATUS_FAILED: return "Failed";
              default: return "";
          }
      }
  }