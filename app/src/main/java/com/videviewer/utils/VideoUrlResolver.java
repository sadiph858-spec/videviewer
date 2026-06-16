package com.videviewer.utils;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * Resolves YouTube / social-media page URLs to direct downloadable stream URLs
 * using the cobalt.tools public API.
 */
public class VideoUrlResolver {

    public interface Callback {
        void onResolved(String streamUrl, String thumbnailUrl, String title);
        void onError(String message);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Supported platform check */
    public static boolean isSupportedPlatform(String url) {
        if (url == null) return false;
        return url.contains("youtube.com") || url.contains("youtu.be")
            || url.contains("instagram.com") || url.contains("facebook.com")
            || url.contains("fb.watch")       || url.contains("tiktok.com")
            || url.contains("twitter.com")    || url.contains("x.com")
            || url.contains("vimeo.com")      || url.contains("dailymotion.com");
    }

    /** Extract YouTube video ID from any YouTube URL format */
    public static String extractYouTubeId(String url) {
        if (url == null) return null;
        String id = null;
        if (url.contains("youtu.be/")) {
            int start = url.indexOf("youtu.be/") + 9;
            int end   = url.indexOf('?', start);
            id = end > start ? url.substring(start, end) : url.substring(start);
        } else if (url.contains("v=")) {
            int start = url.indexOf("v=") + 2;
            int end   = url.indexOf('&', start);
            id = end > start ? url.substring(start, end) : url.substring(start);
        } else if (url.contains("/shorts/")) {
            int start = url.indexOf("/shorts/") + 8;
            int end   = url.indexOf('?', start);
            id = end > start ? url.substring(start, end) : url.substring(start);
        }
        return (id != null && id.length() >= 11) ? id.substring(0, 11) : null;
    }

    /** Thumbnail URL for a YouTube video ID */
    public static String youtubeThumbnail(String videoId) {
        if (videoId == null) return null;
        return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
    }

    /**
     * Resolve a supported page URL → direct stream URL.
     * Calls back on the main thread.
     */
    public static void resolve(String pageUrl, Callback callback) {
        executor.execute(() -> {
            try {
                URL api = new URL("https://api.cobalt.tools/");
                HttpURLConnection conn = (HttpURLConnection) api.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("url", pageUrl);
                body.put("videoQuality", "720");
                body.put("filenameStyle", "basic");
                body.put("downloadMode", "auto");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                        code >= 200 && code < 400 ? conn.getInputStream()
                                                   : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject resp = new JSONObject(sb.toString());
                String status = resp.optString("status", "");

                if ("stream".equals(status) || "redirect".equals(status)
                        || "tunnel".equals(status)) {
                    String streamUrl = resp.getString("url");
                    // Build thumbnail URL for YouTube
                    String ytId = extractYouTubeId(pageUrl);
                    String thumb = youtubeThumbnail(ytId);
                    String title = resp.optString("filename", "video_" + System.currentTimeMillis() + ".mp4");
                    mainHandler.post(() -> callback.onResolved(streamUrl, thumb, title));
                } else {
                    String err = resp.optString("error", "Could not resolve video URL");
                    mainHandler.post(() -> callback.onError(err));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
