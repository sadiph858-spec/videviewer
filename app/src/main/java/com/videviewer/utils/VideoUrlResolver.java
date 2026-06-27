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

    // Updated Invidious instances (June 2025)
    private static final String[] INVIDIOUS = {
        "https://inv.nadeko.net",
        "https://yewtu.be",
        "https://invidious.privacydev.net",
        "https://yt.artemislena.eu",
        "https://invidious.perennialte.ch",
        "https://invidious.nerdvpn.de",
        "https://invidious.io.lol",
        "https://inv.tux.pizza",
        "https://invidious.fdn.fr",
        "https://iv.datura.network",
        "https://invidious.slipfox.xyz",
        "https://invidious.reallyaweso.me",
        "https://vid.puffyan.us",
        "https://invidious.flokinet.to"
    };

    // Updated Piped instances (June 2025)
    private static final String[] PIPED = {
        "https://api.piped.yt",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://piped-api.garudalinux.org",
        "https://watchapi.whatever.social",
        "https://piped.syncpundit.io"
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
                    // Direct video URL — pass through as-is
                    mainHandler.post(() -> callback.onResolved(
                        pageUrl, null, guessFilename(pageUrl)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    private static String guessFilename(String url) {
        try {
            String p = url.split("\\?")[0];
            String n = p.substring(p.lastIndexOf('/') + 1);
            return n.isEmpty() ? "video_" + System.currentTimeMillis() + ".mp4" : n;
        } catch (Exception e) {
            return "video_" + System.currentTimeMillis() + ".mp4";
        }
    }

    private static void resolveYouTube(String videoId, Callback callback) {
        String thumbnail = youtubeThumbnail(videoId);

        // ── Try Invidious instances (formatStreams + adaptiveFormats) ──
        for (String base : INVIDIOUS) {
            try {
                String url = base + "/api/v1/videos/" + videoId
                    + "?fields=title,formatStreams,adaptiveFormats";
                String body = fetch(url);
                if (body == null || body.isEmpty()) continue;

                JSONObject j = new JSONObject(body);
                String rawTitle = j.optString("title", "video_" + videoId);
                String title = sanitize(rawTitle) + ".mp4";

                // Try formatStreams first (muxed video+audio)
                JSONArray formatStreams = j.optJSONArray("formatStreams");
                if (formatStreams != null && formatStreams.length() > 0) {
                    String best = pickBestStream(formatStreams, "quality");
                    if (best != null && !best.isEmpty()) {
                        final String fu = best, ft = thumbnail, fn = title;
                        mainHandler.post(() -> callback.onResolved(fu, ft, fn));
                        return;
                    }
                }

                // Fallback: adaptiveFormats (video-only, still downloadable)
                JSONArray adaptiveFormats = j.optJSONArray("adaptiveFormats");
                if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                    String best = pickBestStream(adaptiveFormats, "qualityLabel");
                    if (best != null && !best.isEmpty()) {
                        final String fu = best, ft = thumbnail, fn = title;
                        mainHandler.post(() -> callback.onResolved(fu, ft, fn));
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // ── Try Piped instances ──
        for (String base : PIPED) {
            try {
                String url = base + "/streams/" + videoId;
                String body = fetch(url);
                if (body == null || body.isEmpty()) continue;

                JSONObject j = new JSONObject(body);
                String rawTitle = j.optString("title", "video_" + videoId);
                String title = sanitize(rawTitle) + ".mp4";
                String thumb = j.optString("thumbnailUrl", thumbnail);

                JSONArray videoStreams = j.optJSONArray("videoStreams");
                String best = null;

                if (videoStreams != null) {
                    // Prefer muxed MPEG_4 at 720p/480p
                    for (int i = 0; i < videoStreams.length(); i++) {
                        JSONObject s = videoStreams.getJSONObject(i);
                        boolean videoOnly = s.optBoolean("videoOnly", true);
                        String fmt = s.optString("format", "");
                        String q   = s.optString("quality", "");
                        if (!videoOnly && fmt.contains("MPEG_4")
                                && (q.contains("720") || q.contains("480"))) {
                            best = s.optString("url");
                            break;
                        }
                    }
                    // Fallback: any non-video-only
                    if (best == null) {
                        for (int i = 0; i < videoStreams.length(); i++) {
                            JSONObject s = videoStreams.getJSONObject(i);
                            if (!s.optBoolean("videoOnly", true)) {
                                best = s.optString("url");
                                break;
                            }
                        }
                    }
                    // Last resort: first stream
                    if (best == null && videoStreams.length() > 0)
                        best = videoStreams.getJSONObject(0).optString("url");
                }

                if (best != null && !best.isEmpty()) {
                    final String fu = best;
                    final String ft = (thumb != null && !thumb.isEmpty()) ? thumb : thumbnail;
                    final String fn = title;
                    mainHandler.post(() -> callback.onResolved(fu, ft, fn));
                    return;
                }
            } catch (Exception ignored) {}
        }

        mainHandler.post(() -> callback.onError(
            "YouTube ডাউনলোড করা যাচ্ছে না। সরাসরি MP4 লিংক paste করুন।"));
    }

    private static String pickBestStream(JSONArray streams, String qualityKey) throws Exception {
        String best720 = null, best480 = null, best360 = null, bestAny = null;
        for (int i = 0; i < streams.length(); i++) {
            JSONObject s = streams.getJSONObject(i);
            String q = s.optString(qualityKey, "").toLowerCase();
            String u = s.optString("url", "");
            String type = s.optString("type", s.optString("mimeType", ""));
            // Skip audio-only streams
            if (type.contains("audio") && !type.contains("video")) continue;
            if (u.isEmpty()) continue;
            if (q.contains("720")) best720 = u;
            else if (q.contains("480")) best480 = u;
            else if (q.contains("360")) best360 = u;
            if (bestAny == null) bestAny = u;
        }
        if (best720  != null) return best720;
        if (best480  != null) return best480;
        if (best360  != null) return best360;
        return bestAny;
    }

    private static String fetch(String urlStr) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) VidViewer/3.0");
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            int code = c.getResponseCode();
            if (code != 200) return null;
            BufferedReader br = new BufferedReader(
                new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String sanitize(String name) {
        if (name == null) return "video";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\s+", " ").trim();
    }
}
