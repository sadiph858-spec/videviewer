package com.videviewer.utils;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Resolves YouTube URLs to direct downloadable stream URLs
 * using Invidious public instances (no authentication required).
 */
public class VideoUrlResolver {

    public interface Callback {
        void onResolved(String streamUrl, String thumbnailUrl, String title);
        void onError(String message);
    }

    private static final String[] INVIDIOUS_INSTANCES = {
        "https://inv.tux.pizza",
        "https://invidious.privacydev.net",
        "https://yt.artemislena.eu",
        "https://invidious.io.lol",
        "https://invidious.nerdvpn.de"
    };

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static boolean isSupportedPlatform(String url) {
        if (url == null) return false;
        return url.contains("youtube.com") || url.contains("youtu.be")
            || url.contains("vimeo.com") || url.contains("dailymotion.com");
    }

    public static String extractYouTubeId(String url) {
        if (url == null) return null;
        String id = null;
        if (url.contains("youtu.be/")) {
            int s = url.indexOf("youtu.be/") + 9;
            int e = url.indexOf('?', s);
            id = e > s ? url.substring(s, e) : url.substring(s);
        } else if (url.contains("v=")) {
            int s = url.indexOf("v=") + 2;
            int e = url.indexOf('&', s);
            id = e > s ? url.substring(s, e) : url.substring(s);
        } else if (url.contains("/shorts/")) {
            int s = url.indexOf("/shorts/") + 8;
            int e = url.indexOf('?', s);
            id = e > s ? url.substring(s, e) : url.substring(s);
        }
        return (id != null && id.length() >= 11) ? id.substring(0, 11) : null;
    }

    public static String youtubeThumbnail(String videoId) {
        return videoId != null
            ? "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg"
            : null;
    }

    public static void resolve(String pageUrl, Callback callback) {
        executor.execute(() -> {
            try {
                String ytId = extractYouTubeId(pageUrl);
                if (ytId != null) {
                    resolveYouTube(ytId, callback);
                } else {
                    mainHandler.post(() -> callback.onResolved(
                        pageUrl, null,
                        "video_" + System.currentTimeMillis() + ".mp4"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Resolver error: " + e.getMessage()));
            }
        });
    }

    private static void resolveYouTube(String videoId, Callback callback) {
        String thumbnail = youtubeThumbnail(videoId);
        Exception lastError = null;

        for (String instance : INVIDIOUS_INSTANCES) {
            try {
                String apiUrl = instance + "/api/v1/videos/" + videoId
                    + "?fields=title,formatStreams,adaptiveFormats";
                HttpURLConnection conn = openConn(apiUrl);
                int code = conn.getResponseCode();
                if (code != 200) { conn.disconnect(); continue; }

                BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();

                JSONObject resp = new JSONObject(sb.toString());
                String rawTitle = resp.optString("title", "video_" + videoId);
                // Sanitise filename — escape the quote inside the char class
                String title = rawTitle.replaceAll("[\\/:*?\"<>|]", "_") + ".mp4";

                JSONArray streams = resp.optJSONArray("formatStreams");
                String streamUrl = null;
                if (streams != null) {
                    for (int i = 0; i < streams.length(); i++) {
                        JSONObject fmt = streams.getJSONObject(i);
                        String q = fmt.optString("quality", "");
                        if (q.equals("720p") || q.equals("hd720")) {
                            streamUrl = fmt.optString("url");
                            break;
                        }
                    }
                    if (streamUrl == null && streams.length() > 0)
                        streamUrl = streams.getJSONObject(0).optString("url");
                }

                if (streamUrl != null && !streamUrl.isEmpty()) {
                    final String fUrl   = streamUrl;
                    final String fThumb = thumbnail;
                    final String fTitle = title;
                    mainHandler.post(() -> callback.onResolved(fUrl, fThumb, fTitle));
                    return;
                }
            } catch (Exception e) {
                lastError = e;
            }
        }

        final String errMsg = lastError != null ? lastError.getMessage() : "No stream found";
        mainHandler.post(() -> callback.onError("Could not get stream: " + errMsg));
    }

    private static HttpURLConnection openConn(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", "VidViewer/1.0");
        c.setConnectTimeout(12000);
        c.setReadTimeout(15000);
        return c;
    }
}
