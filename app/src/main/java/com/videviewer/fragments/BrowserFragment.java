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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.videviewer.R;
import com.videviewer.databinding.FragmentBrowserBinding;
import com.videviewer.utils.VideoUrlResolver;

public class BrowserFragment extends Fragment {

    private FragmentBrowserBinding binding;
    private static final String HOME_URL = "https://www.google.com";

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
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) "
            + "AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");

        binding.webView.setWebViewClient(new WebViewClient() {
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
        boolean isVideo = VideoUrlResolver.isSupportedPlatform(url)
            || url.endsWith(".mp4") || url.endsWith(".mkv")
            || url.endsWith(".webm") || url.endsWith(".m3u8")
            || url.endsWith(".avi")  || url.endsWith(".mov");
        if (binding == null) return;
        if (isVideo) {
            binding.fabDownload.setVisibility(View.VISIBLE);
            binding.fabDownload.setOnClickListener(v -> handleDownload(url));
        } else {
            binding.fabDownload.setVisibility(View.GONE);
        }
    }

    private void handleDownload(String url) {
        if (getActivity() == null) return;
        FragmentManager fm = requireActivity().getSupportFragmentManager();

        // Try to find already-created DownloadsFragment
        DownloadsFragment dlFrag =
            (DownloadsFragment) fm.findFragmentByTag("tag_downloads");

        if (dlFrag != null && dlFrag.isAdded()) {
            // ── Fragment already exists — show it and call directly ──
            fm.beginTransaction()
                .hide(this)
                .show(dlFrag)
                .commitAllowingStateLoss();
            syncBottomNav();
            // Direct call — no arguments bundle needed
            dlFrag.resolveAndDownload(url);
        } else {
            // ── First time — pass URL via bundle ────────────────────
            if (dlFrag == null) dlFrag = new DownloadsFragment();
            Bundle args = new Bundle();
            args.putString("share_url", url);
            dlFrag.setArguments(args);
            fm.beginTransaction()
                .hide(this)
                .replace(R.id.fragment_container, dlFrag, "tag_downloads")
                .commitAllowingStateLoss();
            syncBottomNav();
        }

        Toast.makeText(requireContext(), "📥 Starting download…", Toast.LENGTH_SHORT).show();
    }

    private void syncBottomNav() {
        if (getActivity() == null) return;
        BottomNavigationView nav = getActivity().findViewById(R.id.bottom_nav);
        if (nav != null) nav.setSelectedItemId(R.id.nav_downloads);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
