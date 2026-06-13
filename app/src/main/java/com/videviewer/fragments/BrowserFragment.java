package com.videviewer.fragments;

  import android.os.Bundle;
  import android.view.KeyEvent;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import android.view.inputmethod.EditorInfo;
  import android.webkit.WebChromeClient;
  import android.webkit.WebSettings;
  import android.webkit.WebView;
  import android.webkit.WebViewClient;
  import android.widget.Toast;
  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import androidx.fragment.app.Fragment;
  import com.videviewer.databinding.FragmentBrowserBinding;

  public class BrowserFragment extends Fragment {
      private FragmentBrowserBinding binding;
      private static final String HOME_URL = "https://www.google.com";

      @Nullable @Override
      public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
          binding = FragmentBrowserBinding.inflate(inflater, container, false);
          return binding.getRoot();
      }

      @Override
      public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
          super.onViewCreated(view, savedInstanceState);
          setupWebView();
          setupAddressBar();
          binding.webView.loadUrl(HOME_URL);
      }

      private void setupWebView() {
          WebSettings settings = binding.webView.getSettings();
          settings.setJavaScriptEnabled(true);
          settings.setDomStorageEnabled(true);
          settings.setLoadWithOverviewMode(true);
          settings.setUseWideViewPort(true);
          settings.setBuiltInZoomControls(true);
          settings.setDisplayZoomControls(false);
          settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

          binding.webView.setWebViewClient(new WebViewClient() {
              @Override
              public void onPageFinished(WebView view, String url) {
                  binding.etUrl.setText(url);
                  binding.progressBar.setVisibility(View.GONE);
                  detectVideo(url);
              }
              @Override
              public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                  binding.progressBar.setVisibility(View.VISIBLE);
                  binding.fabDownload.setVisibility(View.GONE);
              }
          });

          binding.webView.setWebChromeClient(new WebChromeClient() {
              @Override
              public void onProgressChanged(WebView view, int newProgress) {
                  binding.progressBar.setProgress(newProgress);
              }
          });
      }

      private void setupAddressBar() {
          binding.etUrl.setOnEditorActionListener((v, actionId, event) -> {
              if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                  String url = binding.etUrl.getText().toString().trim();
                  if (!url.startsWith("http")) url = "https://" + url;
                  binding.webView.loadUrl(url);
                  return true;
              }
              return false;
          });
          binding.btnBack.setOnClickListener(v -> { if (binding.webView.canGoBack()) binding.webView.goBack(); });
          binding.btnForward.setOnClickListener(v -> { if (binding.webView.canGoForward()) binding.webView.goForward(); });
          binding.btnRefresh.setOnClickListener(v -> binding.webView.reload());
          binding.btnHome.setOnClickListener(v -> binding.webView.loadUrl(HOME_URL));
      }

      private void detectVideo(String url) {
          boolean hasVideo = url.contains("youtube.com") || url.contains("dailymotion.com")
              || url.contains("vimeo.com") || url.contains("dailyhunt.in")
              || url.endsWith(".mp4") || url.endsWith(".mkv") || url.endsWith(".webm");
          binding.fabDownload.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
          if (hasVideo) {
              binding.fabDownload.setOnClickListener(v ->
                  Toast.makeText(requireContext(), "Opening download for: " + url, android.widget.Toast.LENGTH_SHORT).show());
          }
      }

      @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
  }