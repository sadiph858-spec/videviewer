package com.videviewer.utils;

  import android.os.Handler;
  import android.os.Looper;
  import okhttp3.MediaType;
  import okhttp3.OkHttpClient;
  import okhttp3.Request;
  import okhttp3.RequestBody;
  import okhttp3.Response;
  import org.json.JSONArray;
  import org.json.JSONObject;
  import java.util.concurrent.TimeUnit;

  /**
   * Resolves social-media video URLs to direct download links
   * using the free cobalt.tools API.
   * Supported: YouTube, Instagram, TikTok, Twitter/X, Facebook,
   *            Reddit, Twitch, Vimeo, Dailymotion, SoundCloud, Bilibili…
   */
  public class CobaltResolver {

      public interface Callback {
          void onResolved(String directUrl, String filename);
          void onError(String message);
      }

      private static final String API_URL = "https://api.cobalt.tools/";
      private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
          .connectTimeout(12, TimeUnit.SECONDS)
          .readTimeout(18, TimeUnit.SECONDS)
          .build();
      private static final Handler MAIN = new Handler(Looper.getMainLooper());

      /** Async resolve — callback is delivered on the main thread */
      public static void resolve(String url, Callback callback) {
          new Thread(() -> {
              try {
                  String jsonBody = new JSONObject().put("url", url).toString();
                  Request req = new Request.Builder()
                      .url(API_URL)
                      .post(RequestBody.create(jsonBody,
                          MediaType.get("application/json; charset=utf-8")))
                      .addHeader("Accept", "application/json")
                      .addHeader("User-Agent", "VidViewer/3.2.5 (Android)")
                      .build();

                  try (Response resp = CLIENT.newCall(req).execute()) {
                      if (resp.body() == null) {
                          mainErr(callback, "Empty response from server");
                          return;
                      }
                      String raw = resp.body().string();
                      JSONObject obj = new JSONObject(raw);
                      String status = obj.optString("status", "");

                      if ("error".equals(status)) {
                          JSONObject err = obj.optJSONObject("error");
                          String msg = err != null
                              ? err.optString("message", "Unknown error")
                              : obj.optString("error", "Unknown error");
                          mainErr(callback, msg);
                          return;
                      }

                      // stream / redirect / tunnel — single URL
                      String directUrl = obj.optString("url", null);
                      if (directUrl != null && !directUrl.isEmpty()) {
                          String fname = obj.optString("filename", null);
                          MAIN.post(() -> callback.onResolved(directUrl, fname));
                          return;
                      }

                      // picker — multiple quality options: pick best (first)
                      if ("picker".equals(status)) {
                          JSONArray picker = obj.optJSONArray("picker");
                          if (picker != null && picker.length() > 0) {
                              String first = picker.getJSONObject(0).optString("url", null);
                              if (first != null) {
                                  MAIN.post(() -> callback.onResolved(first, null));
                                  return;
                              }
                          }
                      }

                      mainErr(callback, "Could not extract download URL");
                  }
              } catch (Exception e) {
                  mainErr(callback, e.getMessage() != null ? e.getMessage() : "Network error");
              }
          }).start();
      }

      private static void mainErr(Callback cb, String msg) {
          MAIN.post(() -> cb.onError(msg));
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
              || l.contains("soundcloud.com") || l.contains("pinterest.com")
              || l.contains("tumblr.com")    || l.contains("streamable.com");
      }
  }
  