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
import androidx.fragment.app.FragmentManager;
import com.videviewer.databinding.FragmentBrowserBinding;
import com.videviewer.utils.VideoUrlResolver;

public class BrowserFragment extends Fragment {

    private FragmentBrowserBinding binding;
    private static final String HOME_URL = "https://www.google.com";
    private String currentVideoUrl = null;

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

        // If launched with a URL (e.g. from share intent), load it
        Bundle args = getArguments();
        String startUrl = (args != null) ? args.getString("url", HOME_URL) : HOME_URL;
        binding.webView.loadUrl(startUrl);
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
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                binding.etUrl.setText(url);
                binding.progressBar.setVisibility(View.GONE);
                detectVideoPage(url);
            }
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap fav) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.fabDownload.setVisibility(View.GONE);
                currentVideoUrl = null;
            }
        });

        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int p) {
                binding.progressBar.setProgress(p);
            }
        });
        // ── DownloadManager: কোনো ফাইল-লিংক ক্লিক হলে browser না খুলে ডাউনলোড করে ──
        binding.webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType);
                String cookie   = android.webkit.CookieManager.getInstance().getCookie(url);
                android.app.DownloadManager.Request req =
                    new android.app.DownloadManager.Request(android.net.Uri.parse(url));
                if (mimeType != null) req.setMimeType(mimeType);
                if (cookie   != null) req.addRequestHeader("cookie", cookie);
                req.addRequestHeader("User-Agent", userAgent);
                req.setDescription("Downloading…");
                req.setTitle(fileName);
                req.setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
                android.app.DownloadManager dm =
                    (android.app.DownloadManager) requireContext()
                        .getSystemService(android.content.Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(req);
                    Toast.makeText(requireContext(),
                        "Downloading: " + fileName, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Download failed", Toast.LENGTH_SHORT).show();
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
        binding.btnBack.setOnClickListener(v -> { if (binding.webView.canGoBack()) binding.webView.goBack(); });
        binding.btnForward.setOnClickListener(v -> { if (binding.webView.canGoForward()) binding.webView.goForward(); });
        binding.btnRefresh.setOnClickListener(v -> binding.webView.reload());
        binding.btnHome.setOnClickListener(v -> binding.webView.loadUrl(HOME_URL));
    }

    private void detectVideoPage(String url) {
        boolean isVideoPage = VideoUrlResolver.isSupportedPlatform(url)
            || url.endsWith(".mp4") || url.endsWith(".mkv")
            || url.endsWith(".webm") || url.endsWith(".m3u8");

        if (isVideoPage) {
            currentVideoUrl = url;
            binding.fabDownload.setVisibility(View.VISIBLE);
            binding.fabDownload.setOnClickListener(v -> handleDownload(url));
        } else {
            binding.fabDownload.setVisibility(View.GONE);
            currentVideoUrl = null;
        }
    }

    /** Routes to DownloadsFragment and triggers resolveAndDownload */
    private void handleDownload(String url) {
        FragmentManager fm = requireActivity().getSupportFragmentManager();

        // Find or create DownloadsFragment
        DownloadsFragment dlFrag = (DownloadsFragment) fm.findFragmentByTag("tag_downloads");
        if (dlFrag == null) dlFrag = new DownloadsFragment();

        // Pass URL via arguments
        Bundle args = new Bundle();
        args.putString("share_url", url);
        dlFrag.setArguments(args);

        // Switch to Downloads tab
        fm.beginTransaction()
            .hide(this)
            .replace(com.videviewer.R.id.fragment_container, dlFrag, "tag_downloads")
            .commitAllowingStateLoss();

        // Update bottom nav selection
        if (getActivity() != null) {
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                getActivity().findViewById(com.videviewer.R.id.bottom_nav);
            if (nav != null) nav.setSelectedItemId(com.videviewer.R.id.nav_downloads);
        }
        Toast.makeText(requireContext(), "Starting download…", Toast.LENGTH_SHORT).show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
