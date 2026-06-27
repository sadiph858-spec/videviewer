package com.videviewer.activities;

  import android.app.DownloadManager;
  import android.net.Uri;
  import android.os.Bundle;
  import android.os.Environment;
  import android.view.KeyEvent;
  import android.view.View;
  import android.view.inputmethod.EditorInfo;
  import android.webkit.CookieManager;
  import android.webkit.URLUtil;
  import android.webkit.WebSettings;
  import android.webkit.WebView;
  import android.webkit.WebViewClient;
  import android.widget.EditText;
  import android.widget.ImageButton;
  import android.widget.ProgressBar;
  import android.widget.Toast;

  import androidx.appcompat.app.AppCompatActivity;
  import androidx.appcompat.widget.Toolbar;

  import com.videviewer.R;

  /**
   * BrowserActivity – in-app WebView browser with download support.
   */
  public class BrowserActivity extends AppCompatActivity {

      private WebView webView;
      private EditText etAddress;
      private ProgressBar progressBar;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          try {
              super.onCreate(savedInstanceState);
              setContentView(R.layout.activity_browser);

              Toolbar toolbar = findViewById(R.id.toolbar);
              setSupportActionBar(toolbar);
              if (getSupportActionBar() != null) {
                  getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                  getSupportActionBar().setTitle("Browser");
              }

              etAddress   = findViewById(R.id.et_address);
              progressBar = findViewById(R.id.progress_bar);
              webView     = findViewById(R.id.web_view);

              setupWebView();
              setupAddressBar();
              setupNavButtons();

              loadUrl("https://www.google.com");
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      private void setupWebView() {
          try {
              WebSettings settings = webView.getSettings();
              settings.setJavaScriptEnabled(true);
              settings.setDomStorageEnabled(true);
              settings.setAllowFileAccess(true);
              settings.setUserAgentString(
                  "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                  "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

              webView.setWebViewClient(new WebViewClient() {
                  @Override
                  public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                      try {
                          progressBar.setVisibility(View.VISIBLE);
                          etAddress.setText(url);
                      } catch (Exception e) { e.printStackTrace(); }
                  }

                  @Override
                  public void onPageFinished(WebView view, String url) {
                      try {
                          progressBar.setVisibility(View.GONE);
                          etAddress.setText(url);
                      } catch (Exception e) { e.printStackTrace(); }
                  }
              });

              webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
                  try {
                      DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                      request.setMimeType(mimetype);
                      String cookies = CookieManager.getInstance().getCookie(url);
                      request.addRequestHeader("cookie", cookies);
                      request.addRequestHeader("User-Agent", userAgent);
                      String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                      request.setTitle(fileName);
                      request.allowScanningByMediaScanner();
                      request.setNotificationVisibility(
                          DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                      request.setDestinationInExternalPublicDir(
                          Environment.DIRECTORY_DOWNLOADS, fileName);
                      DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                      dm.enqueue(request);
                      Toast.makeText(this, "Downloading " + fileName, Toast.LENGTH_LONG).show();
                  } catch (Exception e) {
                      e.printStackTrace();
                  }
              });
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      private void setupAddressBar() {
          try {
              etAddress.setOnEditorActionListener((v, actionId, event) -> {
                  try {
                      if (actionId == EditorInfo.IME_ACTION_GO
                              || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                          loadUrl(etAddress.getText().toString().trim());
                          return true;
                      }
                  } catch (Exception e) { e.printStackTrace(); }
                  return false;
              });
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      private void setupNavButtons() {
          try {
              ImageButton btnBack    = findViewById(R.id.btn_back);
              ImageButton btnForward = findViewById(R.id.btn_forward);
              ImageButton btnRefresh = findViewById(R.id.btn_refresh);

              btnBack.setOnClickListener(v -> {
                  try { webView.goBack(); } catch (Exception e) { e.printStackTrace(); }
              });
              btnForward.setOnClickListener(v -> {
                  try { webView.goForward(); } catch (Exception e) { e.printStackTrace(); }
              });
              btnRefresh.setOnClickListener(v -> {
                  try { webView.reload(); } catch (Exception e) { e.printStackTrace(); }
              });
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      private void loadUrl(String raw) {
          try {
              String url = raw;
              if (!url.startsWith("http://") && !url.startsWith("https://")) {
                  if (url.contains(".") && !url.contains(" ")) {
                      url = "https://" + url;
                  } else {
                      url = "https://www.google.com/search?q=" + Uri.encode(url);
                  }
              }
              webView.loadUrl(url);
          } catch (Exception e) {
              e.printStackTrace();
          }
      }

      @Override
      public void onBackPressed() {
          try {
              if (webView.canGoBack()) {
                  webView.goBack();
              } else {
                  super.onBackPressed();
              }
          } catch (Exception e) {
              e.printStackTrace();
              super.onBackPressed();
          }
      }

      @Override
      public boolean onSupportNavigateUp() {
          try { onBackPressed(); } catch (Exception e) { e.printStackTrace(); }
          return true;
      }
  }
  