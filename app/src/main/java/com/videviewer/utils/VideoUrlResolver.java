package com.videviewer.utils;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Resolves YouTube URLs to direct downloadable stream URLs.
 *
 * Priority order (confirmed by live testing, June 2025):
 *  1. ANDROID_KIDS InnerTube client  — returns direct URLs, no pot token
 *  2. ANDROID_CREATOR InnerTube      — fallback
 *  3. Invidious public instances     — fallback
 *  4. Piped public instances         — last resort
 */
public class VideoUrlResolver {

    public interface Callback {
        void onResolved(String streamUrl, String thumbnailUrl, String title);
        void onError(String message);
    }

    private static final String INNERTUBE_URL =
        "https://www.youtube.com/youtubei/v1/player?prettyPrint=false";

    // ── Client configs (name, version, clientNum, userAgent) ─
    private static final String[][] INNERTUBE_CLIENTS = {
        // ANDROID_KIDS — confirmed working, no pot needed
        {
            "ANDROID_KIDS", "9.17.3", "27",
            "com.google.android.apps.youtube.kids/9.17.3 (Linux; U; Android 11) gzip"
        },
        // ANDROID_CREATOR — second try
        {
            "ANDROID_CREATOR", "22.30.100", "14",
            "com.google.android.apps.youtube.creator/22.30.100 (Linux; U; Android 11) gzip"
        },
    };

    private static final String[] INVIDIOUS = {
        "https://inv.tux.pizza",
        "https://invidious.privacydev.net",
        "https://yt.artemislena.eu",
        "https://invidious.io.lol",
        "https://invidious.fdn.fr",
        "https://iv.datura.network"
    };

    private static final String[] PIPED = {
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://piped-api.garudalinux.org"
    };

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Public API ───────────────────────────────────────────
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
        } else if (url.contains("/live/")) {
            int s = url.indexOf("/live/") + 6;
            int e = url.indexOf('?', s);
            id = e > s ? url.substring(s, e) : url.substring(s);
        }
        if (id != null && id.length() >= 11) id = id.substring(0, 11);
        return (id != null && id.length() == 11) ? id : null;
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
                    // Direct URL — pass through as-is
                    mainHandler.post(() -> callback.onResolved(
                        pageUrl, null,
                        "video_" + System.currentTimeMillis() + ".mp4"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Resolver: " + e.getMessage()));
            }
        });
    }

    // ── Core YouTube resolution ───────────────────────────────
    private static void resolveYouTube(String videoId, Callback callback) {
        String thumb = youtubeThumbnail(videoId);

        // 1 & 2 — InnerTube clients
        for (String[] cfg : INNERTUBE_CLIENTS) {
            StreamResult r = tryInnerTube(videoId, cfg[0], cfg[1], cfg[2], cfg[3]);
            if (r != null) { deliver(r, thumb, callback); return; }
        }

        // 3 — Invidious
        for (String base : INVIDIOUS) {
            StreamResult r = tryInvidious(base, videoId);
            if (r != null) { deliver(r, thumb, callback); return; }
        }

        // 4 — Piped
        for (String base : PIPED) {
            StreamResult r = tryPiped(base, videoId);
            if (r != null) { deliver(r, thumb, callback); return; }
        }

        mainHandler.post(() -> callback.onError(
            "Could not get stream. Try: copy the YouTube URL and paste in + ADD URL"));
    }

    private static void deliver(StreamResult r, String fallbackThumb, Callback cb) {
        final String u = r.url;
        final String t = r.thumb != null ? r.thumb : fallbackThumb;
        final String n = r.title;
        mainHandler.post(() -> cb.onResolved(u, t, n));
    }

    // ── InnerTube POST ───────────────────────────────────────
    private static StreamResult tryInnerTube(
            String videoId, String clientName, String clientVersion,
            String clientNum, String userAgent) {
        try {
            // Build JSON body manually (no Gson dependency)
            String body = "{\"context\":{\"client\":{"
                + "\"clientName\":\"" + clientName + "\","
                + "\"clientVersion\":\"" + clientVersion + "\","
                + "\"androidSdkVersion\":30,"
                + "\"hl\":\"en\",\"timeZone\":\"UTC\",\"utcOffsetMinutes\":0"
                + "}},\"videoId\":\"" + videoId + "\","
                + "\"contentCheckOk\":true,\"racyCheckOk\":true}";

            HttpURLConnection c =
                (HttpURLConnection) new URL(INNERTUBE_URL).openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("User-Agent", userAgent);
            c.setRequestProperty("X-YouTube-Client-Name", clientNum);
            c.setRequestProperty("X-YouTube-Client-Version", clientVersion);
            c.setConnectTimeout(12000);
            c.setReadTimeout(15000);
            c.setDoOutput(true);

            DataOutputStream out = new DataOutputStream(c.getOutputStream());
            out.write(body.getBytes("UTF-8"));
            out.flush();
            out.close();

            int code = c.getResponseCode();
            if (code != 200) { c.disconnect(); return null; }

            String json = readAll(c);
            c.disconnect();

            JSONObject resp = new JSONObject(json);

            // Check playability
            JSONObject ps = resp.optJSONObject("playabilityStatus");
            if (ps != null) {
                String status = ps.optString("status", "");
                if ("ERROR".equals(status) || "LOGIN_REQUIRED".equals(status)
                        || "UNPLAYABLE".equals(status)) return null;
            }

            // Title
            JSONObject details = resp.optJSONObject("videoDetails");
            String rawTitle = (details != null)
                ? details.optString("title", "video_" + videoId)
                : "video_" + videoId;
            String title = sanitize(rawTitle) + ".mp4";

            // Muxed formats (video + audio together) — prefer 720p
            JSONObject sd = resp.optJSONObject("streamingData");
            if (sd == null) return null;

            JSONArray formats = sd.optJSONArray("formats");
            if (formats == null || formats.length() == 0) {
                // Some kids videos only have adaptiveFormats; try video-only as fallback
                JSONArray adaptive = sd.optJSONArray("adaptiveFormats");
                if (adaptive != null) {
                    for (int i = 0; i < adaptive.length(); i++) {
                        JSONObject f = adaptive.getJSONObject(i);
                        String u = f.optString("url", "");
                        String mime = f.optString("mimeType", "");
                        if (!u.isEmpty() && mime.startsWith("video/mp4")) {
                            return new StreamResult(u, null, title);
                        }
                    }
                }
                return null;
            }

            String best720 = null, best360 = null, bestAny = null;
            for (int i = 0; i < formats.length(); i++) {
                JSONObject f = formats.getJSONObject(i);
                String u = f.optString("url", "");
                if (u.isEmpty()) continue; // skip cipher entries
                int itag = f.optInt("itag", 0);
                if (itag == 22)           best720 = u;
                else if (itag == 18)      best360 = u;
                if (bestAny == null)      bestAny = u;
            }

            String chosen = best720 != null ? best720
                          : best360 != null ? best360
                          : bestAny;
            return chosen != null ? new StreamResult(chosen, null, title) : null;

        } catch (Exception e) { return null; }
    }

    // ── Invidious ────────────────────────────────────────────
    private static StreamResult tryInvidious(String base, String videoId) {
        try {
            HttpURLConnection c = openGet(
                base + "/api/v1/videos/" + videoId + "?fields=title,formatStreams");
            if (c == null || c.getResponseCode() != 200) return null;
            String json = readAll(c); c.disconnect();
            JSONObject j = new JSONObject(json);
            String title = sanitize(j.optString("title", "video_" + videoId)) + ".mp4";
            JSONArray streams = j.optJSONArray("formatStreams");
            String url = pickBest(streams, "quality");
            return url != null ? new StreamResult(url, null, title) : null;
        } catch (Exception e) { return null; }
    }

    // ── Piped ────────────────────────────────────────────────
    private static StreamResult tryPiped(String base, String videoId) {
        try {
            HttpURLConnection c = openGet(base + "/streams/" + videoId);
            if (c == null || c.getResponseCode() != 200) return null;
            String json = readAll(c); c.disconnect();
            JSONObject j = new JSONObject(json);
            String title = sanitize(j.optString("title", "video_" + videoId)) + ".mp4";
            String thumb = j.optString("thumbnailUrl", null);
            JSONArray streams = j.optJSONArray("videoStreams");
            String best = null;
            if (streams != null) {
                for (int i = 0; i < streams.length(); i++) {
                    JSONObject s = streams.getJSONObject(i);
                    if (!s.optBoolean("videoOnly", true)) {
                        String u = s.optString("url", "");
                        String q = s.optString("quality", "");
                        if (!u.isEmpty()) {
                            if (q.contains("720") || q.contains("480")) { best = u; break; }
                            if (best == null) best = u;
                        }
                    }
                }
            }
            return best != null ? new StreamResult(best, thumb, title) : null;
        } catch (Exception e) { return null; }
    }

    // ── Helpers ──────────────────────────────────────────────
    private static String pickBest(JSONArray arr, String key) throws Exception {
        if (arr == null) return null;
        String b720 = null, any = null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String u = o.optString("url", "");
            if (u.isEmpty()) continue;
            String q = o.optString(key, "");
            if (q.contains("720")) b720 = u;
            if (any == null) any = u;
        }
        return b720 != null ? b720 : any;
    }

    private static HttpURLConnection openGet(String urlStr) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestProperty("User-Agent", "VidViewer/3.0");
            c.setConnectTimeout(10000);
            c.setReadTimeout(12000);
            return c;
        } catch (Exception e) { return null; }
    }

    private static String readAll(HttpURLConnection c) throws Exception {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    private static String sanitize(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").trim();
    }

    private static class StreamResult {
        final String url, thumb, title;
        StreamResult(String url, String thumb, String title) {
            this.url = url; this.thumb = thumb; this.title = title;
        }
    }
}
