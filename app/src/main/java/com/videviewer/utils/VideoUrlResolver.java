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

public class VideoUrlResolver {

    public interface Callback {
        void onResolved(String streamUrl, String thumbnailUrl, String title);
        void onError(String message);
    }

    private static final String[] INVIDIOUS = {
        "https://inv.tux.pizza",
        "https://invidious.privacydev.net",
        "https://yt.artemislena.eu",
        "https://invidious.io.lol",
        "https://invidious.nerdvpn.de",
        "https://invidious.fdn.fr",
        "https://iv.datura.network"
    };

    private static final String[] PIPED = {
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://piped-api.garudalinux.org",
        "https://watchapi.whatever.social"
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
        } else if (url.contains("/embed/")) {
            int s = url.indexOf("/embed/") + 7;
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
                        pageUrl, null, "video_" + System.currentTimeMillis() + ".mp4"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private static void resolveYouTube(String videoId, Callback callback) {
        String thumbnail = youtubeThumbnail(videoId);

        // ── Try Invidious instances ──────────────────────────────
        for (String base : INVIDIOUS) {
            try {
                String url = base + "/api/v1/videos/" + videoId
                    + "?fields=title,formatStreams";
                String body = fetch(url);
                if (body == null) continue;

                JSONObject j = new JSONObject(body);
                String rawTitle = j.optString("title", "video_" + videoId);
                String title = sanitize(rawTitle) + ".mp4";
                JSONArray streams = j.optJSONArray("formatStreams");
                if (streams == null || streams.length() == 0) continue;

                String best = pickBestStream(streams, "quality");
                if (best != null) {
                    final String fu = best, ft = thumbnail, fn = title;
                    mainHandler.post(() -> callback.onResolved(fu, ft, fn));
                    return;
                }
            } catch (Exception ignored) {}
        }

        // ── Try Piped instances ─────────────────────────────────
        for (String base : PIPED) {
            try {
                String url = base + "/streams/" + videoId;
                String body = fetch(url);
                if (body == null) continue;

                JSONObject j = new JSONObject(body);
                String rawTitle = j.optString("title", "video_" + videoId);
                String title = sanitize(rawTitle) + ".mp4";
                String thumb = j.optString("thumbnailUrl", thumbnail);

                // Piped: videoStreams contains muxed and video-only.
                // Look for MPEG_4 non-video-only first (muxed).
                JSONArray videoStreams = j.optJSONArray("videoStreams");
                String best = null;
                if (videoStreams != null) {
                    for (int i = 0; i < videoStreams.length(); i++) {
                        JSONObject s = videoStreams.getJSONObject(i);
                        boolean videoOnly = s.optBoolean("videoOnly", true);
                        String fmt = s.optString("format", "");
                        String q = s.optString("quality", "");
                        if (!videoOnly && fmt.contains("MPEG_4")
                            && (q.contains("720") || q.contains("480"))) {
                            best = s.optString("url");
                            break;
                        }
                    }
                    // fallback: first non-video-only
                    if (best == null) {
                        for (int i = 0; i < videoStreams.length(); i++) {
                            JSONObject s = videoStreams.getJSONObject(i);
                            if (!s.optBoolean("videoOnly", true)) {
                                best = s.optString("url");
                                break;
                            }
                        }
                    }
                    // last resort: first stream
                    if (best == null && videoStreams.length() > 0)
                        best = videoStreams.getJSONObject(0).optString("url");
                }

                if (best != null && !best.isEmpty()) {
                    final String fu = best, ft = thumb, fn = title;
                    mainHandler.post(() -> callback.onResolved(fu, ft, fn));
                    return;
                }
            } catch (Exception ignored) {}
        }

        mainHandler.post(() -> callback.onError(
            "Could not get video stream. Try copying the direct video URL."));
    }

    private static String pickBestStream(JSONArray streams, String qualityKey) throws Exception {
        String best720 = null, bestAny = null;
        for (int i = 0; i < streams.length(); i++) {
            JSONObject s = streams.getJSONObject(i);
            String q = s.optString(qualityKey, "");
            String u = s.optString("url", "");
            if (u.isEmpty()) continue;
            if (q.contains("720") || q.contains("hd720")) best720 = u;
            if (bestAny == null) bestAny = u;
        }
        return best720 != null ? best720 : bestAny;
    }

    private static String fetch(String urlStr) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestProperty("User-Agent", "VidViewer/2.0");
            c.setConnectTimeout(10000);
            c.setReadTimeout(12000);
            if (c.getResponseCode() != 200) { c.disconnect(); return null; }
            BufferedReader br = new BufferedReader(
                new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close(); c.disconnect();
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\s+", " ").trim();
    }
}
