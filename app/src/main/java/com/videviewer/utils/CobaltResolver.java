package com.videviewer.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

/**
 * Resolves social-media video URLs to direct download links via cobalt.
 *
 * The official api.cobalt.tools requires an API key since March 2024.
 * This resolver tries multiple community-hosted cobalt instances that still
 * operate without authentication, falling back through each in order.
 */
public class CobaltResolver {
    private static final String TAG = "CobaltResolver";

    public interface Callback {
        void onResolved(String directUrl, String filename);
        void onError(String message);
    }

    /**
     * Community cobalt instances to try in order.
     * Instances that return 401/403 (auth required) are skipped automatically.
     */
    private static final String[] INSTANCES = {
        "https://cobalt.catvibers.me",
        "https://cob.freetube.org",
        "https://cobalt.catto.xyz",
        "https://cobalt.api.lostfiles.xyz",
        "https://cobalt.aepl.xyz",
        "https://api.cobalt.tools"
    };

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final String BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 11; Pixel 5) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.6099.210 Mobile Safari/537.36";

    /** Async resolve — callback is delivered on the main thread */
    public static void resolve(String url, Callback callback) {
        new Thread(() -> {
            String lastError = "All cobalt instances failed";
            for (String instance : INSTANCES) {
                try {
                    String result = tryInstance(instance, url);
                    if (result != null) {
                        // result format: "url|||filename" or just "url"
                        int sep = result.indexOf("|||");
                        String directUrl = sep > 0 ? result.substring(0, sep) : result;
                        String filename  = sep > 0 ? result.substring(sep + 3) : null;
                        Log.d(TAG, "Resolved via " + instance);
                        MAIN.post(() -> callback.onResolved(directUrl, filename));
                        return;
                    }
                } catch (AuthRequiredException e) {
                    Log.d(TAG, "Instance " + instance + " requires auth, skipping");
                } catch (RateLimitException e) {
                    Log.d(TAG, "Instance " + instance + " rate-limited, skipping");
                } catch (Exception e) {
                    lastError = e.getMessage() != null ? e.getMessage() : "Network error";
                    Log.d(TAG, "Instance " + instance + " failed: " + lastError);
                }
            }
            final String err = lastError;
            MAIN.post(() -> callback.onError(err));
        }).start();
    }

    /**
     * Try a single cobalt instance.
     * @return "directUrl|||filename" on success, null if no URL in response
     * @throws AuthRequiredException if instance requires an API key
     * @throws RateLimitException if instance is rate-limiting
     */
    private static String tryInstance(String baseUrl, String videoUrl) throws Exception {
        // cobalt v10 request body
        JSONObject body = new JSONObject();
        body.put("url", videoUrl);
        body.put("videoQuality", "720");
        body.put("filenameStyle", "pretty");
        body.put("downloadMode", "auto");
        String jsonBody = body.toString();

        String endpoint = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        Request req = new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(jsonBody,
                MediaType.parse("application/json; charset=utf-8")))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", BROWSER_UA)
            .build();

        try (Response resp = CLIENT.newCall(req).execute()) {
            int code = resp.code();
            String raw = resp.body() != null ? resp.body().string() : "";
            Log.d(TAG, baseUrl + " → HTTP " + code +
                " raw=" + raw.substring(0, Math.min(200, raw.length())));

            if (code == 401 || code == 403) throw new AuthRequiredException();
            if (code == 429) throw new RateLimitException();
            if (code == 0 || raw.isEmpty()) return null;
            if (!resp.isSuccessful()) return null;

            JSONObject obj = new JSONObject(raw);
            String status = obj.optString("status", "");

            if ("error".equals(status)) {
                Object errVal = obj.opt("error");
                String errCode = "";
                if (errVal instanceof JSONObject) {
                    errCode = ((JSONObject) errVal).optString("code", "");
                }
                // Auth errors — treat as auth-required so we skip
                if (errCode.contains("auth") || errCode.contains("jwt")) {
                    throw new AuthRequiredException();
                }
                return null;
            }

            // tunnel / redirect / stream
            String directUrl = obj.optString("url", null);
            if (directUrl != null && !directUrl.isEmpty()) {
                String fname = obj.optString("filename", null);
                return (fname != null && !fname.isEmpty())
                    ? directUrl + "|||" + fname
                    : directUrl;
            }

            // picker — take highest-quality item
            if ("picker".equals(status)) {
                JSONArray picker = obj.optJSONArray("picker");
                if (picker != null && picker.length() > 0) {
                    JSONObject item = picker.getJSONObject(0);
                    String pickerUrl = item.optString("url", null);
                    if (pickerUrl != null && !pickerUrl.isEmpty()) return pickerUrl;
                }
            }

            return null;
        }
    }

    /** Returns true for platforms cobalt.tools can handle */
    public static boolean isSupportedUrl(String url) {
        if (url == null) return false;
        String l = url.toLowerCase();
        return l.contains("youtube.com") || l.contains("youtu.be")
            || l.contains("instagram.com") || l.contains("tiktok.com")
            || l.contains("twitter.com")   || l.contains("x.com/")
            || l.contains("facebook.com")  || l.contains("fb.watch")
            || l.contains("reddit.com")    || l.contains("v.redd.it")
            || l.contains("twitch.tv")     || l.contains("vimeo.com")
            || l.contains("dailymotion.com") || l.contains("bilibili.com")
            || l.contains("soundcloud.com") || l.contains("streamable.com");
    }

    private static class AuthRequiredException extends Exception {}
    private static class RateLimitException extends Exception {}
}
