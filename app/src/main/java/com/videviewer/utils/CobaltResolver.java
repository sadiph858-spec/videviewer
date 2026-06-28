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
   * Resolves social-media video URLs to direct download links
   * using the free cobalt.tools API.
   */
  public class CobaltResolver {
      private static final String TAG = "CobaltResolver";

      public interface Callback {
          void onResolved(String directUrl, String filename);
          void onError(String message);
      }

      private static final String API_URL = "https://api.cobalt.tools/";
      private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
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
              try {
                  JSONObject bodyJson = new JSONObject();
                  bodyJson.put("url", url);
                  String jsonBody = bodyJson.toString();

                  Request req = new Request.Builder()
                      .url(API_URL)
                      .post(RequestBody.create(jsonBody,
                          MediaType.parse("application/json; charset=utf-8")))
                      .header("Accept", "application/json")
                      .header("Content-Type", "application/json")
                      .header("Origin", "https://cobalt.tools")
                      .header("Referer", "https://cobalt.tools/")
                      .header("User-Agent", BROWSER_UA)
                      .build();

                  try (Response resp = CLIENT.newCall(req).execute()) {
                      int code = resp.code();
                      String raw = resp.body() != null ? resp.body().string() : "";
                      Log.d(TAG, "HTTP " + code + " raw=" + raw.substring(0, Math.min(300, raw.length())));

                      if (code == 429) {
                          mainErr(callback, "Rate limited by cobalt.tools — please wait a moment and retry");
                          return;
                      }
                      if (code == 403) {
                          mainErr(callback, "cobalt.tools: access denied (403). Try a different video.");
                          return;
                      }
                      if (!resp.isSuccessful() || raw.isEmpty()) {
                          mainErr(callback, "cobalt.tools server error: HTTP " + code);
                          return;
                      }

                      JSONObject obj = new JSONObject(raw);
                      String status = obj.optString("status", "");
                      Log.d(TAG, "status=" + status);

                      if ("error".equals(status)) {
                          // cobalt v1 error format: { error: { code, context } }
                          // cobalt v2 error format: { status:"error", error:{ code } }
                          Object errVal = obj.opt("error");
                          String msg;
                          if (errVal instanceof JSONObject) {
                              JSONObject errObj = (JSONObject) errVal;
                              msg = errObj.optString("code",
                                  errObj.optString("message", "Unknown error"));
                          } else {
                              msg = String.valueOf(errVal);
                          }
                          mainErr(callback, "cobalt: " + msg);
                          return;
                      }

                      // stream / redirect / tunnel
                      String directUrl = obj.optString("url", null);
                      if (directUrl != null && !directUrl.isEmpty()) {
                          String fname = obj.optString("filename", null);
                          Log.d(TAG, "resolved url=" + directUrl.substring(0, Math.min(80, directUrl.length())));
                          MAIN.post(() -> callback.onResolved(directUrl, fname));
                          return;
                      }

                      // picker — take first item
                      if ("picker".equals(status)) {
                          JSONArray picker = obj.optJSONArray("picker");
                          if (picker != null && picker.length() > 0) {
                              JSONObject item = picker.getJSONObject(0);
                              String pickerUrl = item.optString("url", null);
                              if (pickerUrl == null) pickerUrl = item.optString("thumb", null);
                              if (pickerUrl != null) {
                                  final String finalUrl = pickerUrl;
                                  MAIN.post(() -> callback.onResolved(finalUrl, null));
                                  return;
                              }
                          }
                      }

                      mainErr(callback, "cobalt: no download URL in response (status=" + status + ")");
                  }
              } catch (java.net.UnknownHostException e) {
                  mainErr(callback, "No internet or DNS failed. Check your connection.");
              } catch (java.net.SocketTimeoutException e) {
                  mainErr(callback, "cobalt.tools timed out. Check your internet connection.");
              } catch (Exception e) {
                  String msg = e.getMessage();
                  mainErr(callback, msg != null ? msg : "Network error");
              }
          }).start();
      }

      private static void mainErr(Callback cb, String msg) {
          Log.e(TAG, "Error: " + msg);
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
              || l.contains("soundcloud.com") || l.contains("streamable.com");
      }
  }
  