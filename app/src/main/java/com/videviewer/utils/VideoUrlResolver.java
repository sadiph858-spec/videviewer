package com.videviewer.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Resolves YouTube (and other platform) page URLs to direct downloadable stream URLs.
 *
 * Resolution order:
 *   1. YouTube Innertube API (ANDROID_EMBEDDED_PLAYER client) — no third-party, most reliable
 *   2. YouTube Innertube API (ANDROID client) — fallback Innertube client
 *   3. Invidious instances with local=true  — Invidious proxied streams (stable)
 *   4. Piped instances                      — last resort
 */
public class VideoUrlResolver {

    private static final String TAG = "VideoUrlResolver";

    public interface Callback {
        void onResolved(String streamUrl, String thumbnailUrl, String title);
        void onError(String message);
    }

    // ── Innertube ──────────────────────────────────────────────────────────────
    // YouTube's internal API key (embedded in the Android YouTube app, public knowledge).
    private static final String INNERTUBE_KEY  = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
    private static final String INNERTUBE_URL  =
        "https://www.youtube.com/youtubei/v1/player?key=" + INNERTUBE_KEY + "&prettyPrint=false";

    // ── Invidious instances ────────────────────────────────────────────────────
    // local=true makes Invidious proxy the stream — avoids expiring YouTube CDN URLs.
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
        "https://invidious.flokinet.to"
    };

    // ── Piped instances ────────────────────────────────────────────────────────
    private static final String[] PIPED = {
        "https://api.piped.yt",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://piped-api.garudalinux.org",
        "https://watchapi.whatever.social"
    };

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Public API ─────────────────────────────────────────────────────────────

    public static boolean isSupportedPlatform(String url) {
        if (url == null) return false;
        return url.contains("youtube.com") || url.contains("youtu.be")
            || url.contains("vimeo.com")   || url.contains("dailymotion.com");
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
                        pageUrl, null, guessFilename(pageUrl)));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    // ── YouTube resolution chain ───────────────────────────────────────────────

    private static void resolveYouTube(String videoId, Callback callback) {
        String thumbnail = youtubeThumbnail(videoId);

        // ── 1. Innertube ANDROID_EMBEDDED_PLAYER ──────────────────────────────
        try {
            String result = tryInnertube(videoId,
                "ANDROID_EMBEDDED_PLAYER", "17.31.35", "55");
            if (result != null) {
                String[] parts = result.split("\\|\\|\\|", 3);
                String streamUrl = parts[0];
                String title     = parts.length > 1 ? parts[1] : "video_" + videoId + ".mp4";
                Log.d(TAG, "Resolved via Innertube ANDROID_EMBEDDED_PLAYER");
                mainHandler.post(() -> callback.onResolved(streamUrl, thumbnail, title));
                return;
            }
        } catch (Exception e) {
            Log.d(TAG, "Innertube ANDROID_EMBEDDED_PLAYER failed: " + e.getMessage());
        }

        // ── 2. Innertube ANDROID ───────────────────────────────────────────────
        try {
            String result = tryInnertube(videoId,
                "ANDROID", "19.09.37", "3");
            if (result != null) {
                String[] parts = result.split("\\|\\|\\|", 3);
                String streamUrl = parts[0];
                String title     = parts.length > 1 ? parts[1] : "video_" + videoId + ".mp4";
                Log.d(TAG, "Resolved via Innertube ANDROID");
                mainHandler.post(() -> callback.onResolved(streamUrl, thumbnail, title));
                return;
            }
        } catch (Exception e) {
            Log.d(TAG, "Innertube ANDROID failed: " + e.getMessage());
        }

        // ── 3. Invidious instances (local=true → proxied URLs) ─────────────────
        for (String base : INVIDIOUS) {
            try {
                String url = base + "/api/v1/videos/" + videoId
                    + "?fields=title,formatStreams,adaptiveFormats&local=true";
                String body = fetchGet(url);
                if (body == null || body.isEmpty()) continue;

                JSONObject j = new JSONObject(body);
                String rawTitle = j.optString("title", "video_" + videoId);
                String title = sanitize(rawTitle) + ".mp4";

                JSONArray formatStreams = j.optJSONArray("formatStreams");
                if (formatStreams != null && formatStreams.length() > 0) {
                    String best = pickBestStream(formatStreams, "quality");
                    if (best != null && !best.isEmpty()) {
                        Log.d(TAG, "Resolved via Invidious " + base);
                        final String fu = best, ft = thumbnail, fn = title;
                        mainHandler.post(() -> callback.onResolved(fu, ft, fn));
                        return;
                    }
                }
                JSONArray adaptiveFormats = j.optJSONArray("adaptiveFormats");
                if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                    String best = pickBestStream(adaptiveFormats, "qualityLabel");
                    if (best != null && !best.isEmpty()) {
                        Log.d(TAG, "Resolved via Invidious adaptive " + base);
                        final String fu = best, ft = thumbnail, fn = title;
                        mainHandler.post(() -> callback.onResolved(fu, ft, fn));
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // ── 4. Piped instances ─────────────────────────────────────────────────
        for (String base : PIPED) {
            try {
                String url = base + "/streams/" + videoId;
                String body = fetchGet(url);
                if (body == null || body.isEmpty()) continue;

                JSONObject j = new JSONObject(body);
                String rawTitle = j.optString("title", "video_" + videoId);
                String title = sanitize(rawTitle) + ".mp4";
                String thumb = j.optString("thumbnailUrl", thumbnail);

                JSONArray videoStreams = j.optJSONArray("videoStreams");
                if (videoStreams != null) {
                    String best = null;
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
                    if (best == null) {
                        for (int i = 0; i < videoStreams.length(); i++) {
                            JSONObject s = videoStreams.getJSONObject(i);
                            if (!s.optBoolean("videoOnly", true)) {
                                best = s.optString("url");
                                break;
                            }
                        }
                    }
                    if (best == null && videoStreams.length() > 0)
                        best = videoStreams.getJSONObject(0).optString("url");

                    if (best != null && !best.isEmpty()) {
                        Log.d(TAG, "Resolved via Piped " + base);
                        final String fu = best;
                        final String ft = (thumb != null && !thumb.isEmpty()) ? thumb : thumbnail;
                        final String fn = title;
                        mainHandler.post(() -> callback.onResolved(fu, ft, fn));
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        mainHandler.post(() -> callback.onError(
            "YouTube video resolution failed. Check your internet connection and try again."));
    }

    // ── Innertube implementation ───────────────────────────────────────────────

    /**
     * Calls the YouTube Innertube /player API.
     * @return "streamUrl|||title" on success, null on failure
     */
    private static String tryInnertube(String videoId,
                                       String clientName,
                                       String clientVersion,
                                       String clientId) throws Exception {
        JSONObject clientObj = new JSONObject();
        clientObj.put("clientName", clientName);
        clientObj.put("clientVersion", clientVersion);
        clientObj.put("hl", "en");
        clientObj.put("gl", "US");
        if (clientName.contains("ANDROID")) {
            clientObj.put("androidSdkVersion", 30);
            clientObj.put("userAgent",
                "com.google.android.youtube/" + clientVersion + " (Linux; U; Android 11) gzip");
        }

        JSONObject thirdParty = new JSONObject();
        thirdParty.put("embedUrl", "https://www.youtube.com");

        JSONObject context = new JSONObject();
        context.put("client", clientObj);
        if (clientName.contains("EMBEDDED")) {
            context.put("thirdParty", thirdParty);
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("videoId", videoId);
        requestBody.put("context", context);
        requestBody.put("contentCheckOk", true);
        requestBody.put("racyCheckOk", true);

        String ua = clientName.contains("ANDROID")
            ? "com.google.android.youtube/" + clientVersion + " (Linux; U; Android 11) gzip"
            : "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36";

        String responseBody = fetchPost(INNERTUBE_URL, requestBody.toString(), ua, clientId);
        if (responseBody == null || responseBody.isEmpty()) return null;

        JSONObject resp = new JSONObject(responseBody);

        // Check playability
        JSONObject playabilityStatus = resp.optJSONObject("playabilityStatus");
        if (playabilityStatus != null) {
            String status = playabilityStatus.optString("status", "");
            if ("ERROR".equals(status) || "UNPLAYABLE".equals(status)
                    || "LOGIN_REQUIRED".equals(status)) {
                Log.d(TAG, "Innertube playability: " + status);
                return null;
            }
        }

        JSONObject streamingData = resp.optJSONObject("streamingData");
        if (streamingData == null) return null;

        String title = "video_" + videoId;
        JSONObject videoDetails = resp.optJSONObject("videoDetails");
        if (videoDetails != null) {
            String rawTitle = videoDetails.optString("title", title);
            title = sanitize(rawTitle) + ".mp4";
        }

        // Prefer muxed formats (video+audio together)
        JSONArray formats = streamingData.optJSONArray("formats");
        if (formats != null && formats.length() > 0) {
            String best = pickInnertubeStream(formats);
            if (best != null && !best.isEmpty()) {
                return best + "|||" + title;
            }
        }

        // Fallback: adaptive (may be video-only, but sometimes has audio)
        JSONArray adaptive = streamingData.optJSONArray("adaptiveFormats");
        if (adaptive != null && adaptive.length() > 0) {
            String best = pickInnertubeStream(adaptive);
            if (best != null && !best.isEmpty()) {
                return best + "|||" + title;
            }
        }

        return null;
    }

    /** Pick the best stream from Innertube formats array (720p > 480p > 360p > first) */
    private static String pickInnertubeStream(JSONArray formats) throws Exception {
        String best720 = null, best480 = null, best360 = null, bestAny = null;
        for (int i = 0; i < formats.length(); i++) {
            JSONObject f = formats.getJSONObject(i);
            String mimeType = f.optString("mimeType", "");
            // Skip audio-only formats
            if (mimeType.startsWith("audio/")) continue;

            // Get the URL — may be direct "url" or may be in "signatureCipher"
            String url = f.optString("url", null);
            if (url == null || url.isEmpty()) {
                // signatureCipher / cipher — skip (requires JS deobfuscation)
                continue;
            }

            String quality = f.optString("qualityLabel",
                f.optString("quality", "")).toLowerCase();
            if (bestAny == null) bestAny = url;
            if (quality.contains("720") && best720 == null) best720 = url;
            else if (quality.contains("480") && best480 == null) best480 = url;
            else if (quality.contains("360") && best360 == null) best360 = url;
        }
        if (best720 != null) return best720;
        if (best480 != null) return best480;
        if (best360 != null) return best360;
        return bestAny;
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────────

    private static String fetchPost(String urlStr, String body,
                                    String userAgent, String clientId) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            c.setRequestProperty("User-Agent", userAgent);
            c.setRequestProperty("X-YouTube-Client-Name", clientId);
            c.setRequestProperty("X-YouTube-Client-Version",
                extractClientVersion(body));
            c.setRequestProperty("Origin", "https://www.youtube.com");
            c.setRequestProperty("Referer", "https://www.youtube.com/");
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);

            byte[] bodyBytes = body.getBytes("UTF-8");
            c.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = c.getOutputStream()) {
                os.write(bodyBytes);
            }

            int code = c.getResponseCode();
            if (code != 200) return null;
            return readStream(c.getInputStream());
        } catch (Exception e) {
            Log.d(TAG, "fetchPost error: " + e.getMessage());
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String fetchGet(String urlStr) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) VidViewer/5.0");
            c.setRequestProperty("Accept", "application/json");
            c.setConnectTimeout(6000);
            c.setReadTimeout(10000);
            if (c.getResponseCode() != 200) return null;
            return readStream(c.getInputStream());
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readStream(InputStream is) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    /** Extract clientVersion from Innertube JSON body string (for header) */
    private static String extractClientVersion(String body) {
        try {
            int idx = body.indexOf("\"clientVersion\":\"");
            if (idx < 0) return "17.31.35";
            int s = idx + 18;
            int e = body.indexOf('"', s);
            return body.substring(s, e);
        } catch (Exception e) {
            return "17.31.35";
        }
    }

    // ── Stream selection helpers ───────────────────────────────────────────────

    private static String pickBestStream(JSONArray streams, String qualityKey) throws Exception {
        String best720 = null, best480 = null, best360 = null, bestAny = null;
        for (int i = 0; i < streams.length(); i++) {
            JSONObject s = streams.getJSONObject(i);
            String q    = s.optString(qualityKey, "").toLowerCase();
            String u    = s.optString("url", "");
            String type = s.optString("type", s.optString("mimeType", ""));
            if (type.contains("audio") && !type.contains("video")) continue;
            if (u.isEmpty()) continue;
            if (q.contains("720") && best720 == null) best720 = u;
            else if (q.contains("480") && best480 == null) best480 = u;
            else if (q.contains("360") && best360 == null) best360 = u;
            if (bestAny == null) bestAny = u;
        }
        if (best720 != null) return best720;
        if (best480 != null) return best480;
        if (best360 != null) return best360;
        return bestAny;
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

    private static String sanitize(String name) {
        if (name == null) return "video";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\s+", " ").trim();
    }
}
