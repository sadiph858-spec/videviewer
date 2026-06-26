package com.videviewer.fragments;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videviewer.R;
import com.videviewer.databinding.FragmentBrowserBinding;
import com.videviewer.utils.AppConstants;
import com.videviewer.utils.VideoUrlResolver;
import java.io.File;

public class BrowserFragment extends Fragment {

    private FragmentBrowserBinding binding;
    private static final String HOME_URL = "https://www.google.com";

    /** Captured YouTube CDN stream URL from WebView traffic */
    private volatile String capturedStreamUrl = null;
    /** WebView user-agent (needed for DownloadManager) */
    private String webViewUserAgent = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBrowserBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupWebView();
        setupAddressBar();
        Bundle args = getArguments();
        binding.webView.loadUrl(args != null ? args.getString("url", HOME_URL) : HOME_URL);
    }

    private void setupWebView() {
        WebSettings s = binding.webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        // Mobile Chrome UA so YouTube serves mobile player (less DRM)
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        webViewUserAgent = s.getUserAgentString();

        // ── DownloadListener — browser fires this for direct file links ──
        binding.webView.setDownloadListener((url, ua, contentDisposition, mimeType, contentLength) -> {
            if (getContext() == null) return;
            // Exactly what Chrome does: take the URL + cookies → DownloadManager
            String cookies = CookieManager.getInstance().getCookie(url);
            String filename = guessFilename(url, contentDisposition, mimeType);
            startBrowserDownload(url, filename, ua, cookies, null);
            Toast.makeText(getContext(), "⬇️ Downloading: " + filename, Toast.LENGTH_SHORT).show();
        });

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Capture YouTube CDN video URLs that the browser actually plays
                if (url.contains("googlevideo.com")
                        && url.contains("videoplayback")
                        && !url.contains("rtp=")) {   // skip tiny probe requests
                    // Strip 'range' param → tells DM to download the full video
                    capturedStreamUrl = url.replaceAll("[&?]range=[^&]*", "")
                                          .replaceAll("[&?]rn=[^&]*", "");
                }
                return null; // let WebView handle it normally
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (binding == null) return;
                binding.etUrl.setText(url);
                binding.progressBar.setVisibility(View.GONE);
                detectVideoPage(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap fav) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.fabDownload.setVisibility(View.GONE);
                capturedStreamUrl = null; // reset for new page
            }
        });

        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int p) {
                if (binding != null) binding.progressBar.setProgress(p);
            }
        });
    }

    private void setupAddressBar() {
        binding.etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String url = binding.etUrl.getText().toString().trim();
                if (!url.startsWith("http")) url = "https://" + url;
                binding.webView.loadUrl(url);
                return true;
            }
            return false;
        });
        binding.btnBack.setOnClickListener(v -> {
            if (binding != null && binding.webView.canGoBack()) binding.webView.goBack();
        });
        binding.btnForward.setOnClickListener(v -> {
            if (binding != null && binding.webView.canGoForward()) binding.webView.goForward();
        });
        binding.btnRefresh.setOnClickListener(v -> {
            if (binding != null) binding.webView.reload();
        });
        binding.btnHome.setOnClickListener(v -> {
            if (binding != null) binding.webView.loadUrl(HOME_URL);
        });
    }

    private void detectVideoPage(String url) {
        if (binding == null) return;
        boolean isVideo = VideoUrlResolver.isSupportedPlatform(url)
            || url.endsWith(".mp4") || url.endsWith(".mkv")
            || url.endsWith(".webm") || url.endsWith(".m3u8")
            || url.endsWith(".avi")  || url.endsWith(".mov");
        if (isVideo) {
            binding.fabDownload.setVisibility(View.VISIBLE);
            binding.fabDownload.setOnClickListener(v -> handleDownload(url));
        } else {
            binding.fabDownload.setVisibility(View.GONE);
        }
    }

    /**
     * Called when user taps the download FAB.
     *
     * Strategy (browser-like):
     *  1. If the WebView already fetched the actual stream URL from googlevideo.com
     *     → download it directly with WebView cookies (same as what browser does)
     *  2. Otherwise → use InnerTube / Invidious / Piped resolver, then download
     */
    private void handleDownload(String pageUrl) {
        if (getContext() == null) return;

        String cookies = CookieManager.getInstance().getCookie(pageUrl);

        if (capturedStreamUrl != null) {
            // ── Path 1: Browser-intercepted stream URL ───────────────
            String streamUrl = capturedStreamUrl;
            capturedStreamUrl = null;

            String ytId    = VideoUrlResolver.extractYouTubeId(pageUrl);
            String thumb   = VideoUrlResolver.youtubeThumbnail(ytId);
            String filename = (ytId != null ? ytId : "video_" + System.currentTimeMillis()) + ".mp4";

            // Pass cookies so DownloadManager has the same auth as WebView
            String streamCookies = CookieManager.getInstance().getCookie(streamUrl);
            startBrowserDownload(streamUrl, filename, webViewUserAgent,
                streamCookies != null ? streamCookies : cookies, thumb);

            // Also tell DownloadsFragment so it shows up in the list
            notifyDownloadsFragment(streamUrl, filename, thumb);
            Toast.makeText(getContext(), "📥 Download started!", Toast.LENGTH_SHORT).show();

        } else {
            // ── Path 2: Resolve via API, then download ───────────────
            navigateAndResolve(pageUrl);
        }
    }

    // ── System DownloadManager (browser-like behaviour) ────────────
    private void startBrowserDownload(String url, String filename,
                                      String userAgent, String cookies,
                                      String thumbnailUrl) {
        if (getContext() == null) return;
        try {
            File destDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                AppConstants.DOWNLOAD_DIR);
            destDir.mkdirs();

            DownloadManager.Request req =
                new DownloadManager.Request(Uri.parse(url));
            req.setTitle("VidViewer");
            req.setDescription(filename);
            req.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                AppConstants.DOWNLOAD_DIR + "/" + filename);

            // ← The browser always sends these with the download
            if (userAgent != null && !userAgent.isEmpty())
                req.addRequestHeader("User-Agent", userAgent);
            if (cookies != null && !cookies.isEmpty())
                req.addRequestHeader("Cookie", cookies);
            req.addRequestHeader("Referer", "https://www.youtube.com/");
            req.setAllowedOverMetered(true);
            req.setAllowedOverRoaming(true);
            req.allowScanningByMediaScanner();

            DownloadManager dm =
                (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(req);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Download error: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    // ── Tell DownloadsFragment about the new download ───────────────
    private void notifyDownloadsFragment(String url, String filename, String thumb) {
        if (getActivity() == null) return;
        try {
            FragmentManager fm = requireActivity().getSupportFragmentManager();
            DownloadsFragment dlFrag =
                (DownloadsFragment) fm.findFragmentByTag("tag_downloads");
            if (dlFrag != null && dlFrag.isAdded()) {
                dlFrag.addPendingDownload(url, filename, thumb);
            }
        } catch (Exception ignored) {}
    }

    // ── Navigate to Downloads tab and resolve via API ───────────────
    private void navigateAndResolve(String pageUrl) {
        if (getActivity() == null) return;
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        DownloadsFragment dlFrag =
            (DownloadsFragment) fm.findFragmentByTag("tag_downloads");

        if (dlFrag != null && dlFrag.isAdded()) {
            fm.beginTransaction().hide(this).show(dlFrag).commitAllowingStateLoss();
            syncBottomNav();
            dlFrag.resolveAndDownload(pageUrl);
        } else {
            if (dlFrag == null) dlFrag = new DownloadsFragment();
            Bundle args = new Bundle();
            args.putString("share_url", pageUrl);
            dlFrag.setArguments(args);
            fm.beginTransaction()
                .hide(this)
                .replace(R.id.fragment_container, dlFrag, "tag_downloads")
                .commitAllowingStateLoss();
            syncBottomNav();
        }
        Toast.makeText(requireContext(), "📥 Resolving stream…", Toast.LENGTH_SHORT).show();
    }

    private void syncBottomNav() {
        if (getActivity() == null) return;
        BottomNavigationView nav = getActivity().findViewById(R.id.bottom_nav);
        if (nav != null) nav.setSelectedItemId(R.id.nav_downloads);
    }

    // ── Filename helpers ─────────────────────────────────────────────
    private static String guessFilename(String url, String contentDisposition, String mimeType) {
        // Try Content-Disposition header first
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            int i = contentDisposition.indexOf("filename=") + 9;
            String name = contentDisposition.substring(i).replaceAll("\"", "").split(";")[0].trim();
            if (!name.isEmpty()) return name;
        }
        // Try URL path
        String path = Uri.parse(url).getLastPathSegment();
        if (path != null && path.contains(".")) return path;
        // Guess extension from MIME type
        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return "video_" + System.currentTimeMillis() + (ext != null ? "." + ext : ".mp4");
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
