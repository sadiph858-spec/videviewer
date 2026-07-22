package com.videviewer.activities;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.videviewer.R;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.HistoryEntity;
import com.videviewer.utils.AppConstants;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * PlayerActivity — YouTube-style video player.
 *
 * Gestures: double-tap seek, single-tap toggle controls, swipe for
 * brightness/volume, long-press to cycle resize mode.
 * Features: speed control, sleep timer, screen lock, resume position,
 * Picture-in-Picture, watch history.
 */
public class PlayerActivity extends AppCompatActivity {

    private static final long SEEK_MS = 10000;
    private static final long HIDE_CONTROLS_DELAY = 3000;
    private static final long HISTORY_SAVE_INTERVAL = 5000;
    private static final int SWIPE_BRIGHTNESS = 1;
    private static final int SWIPE_VOLUME = 2;
    private static final float[] SPEEDS = {0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};

    // Player
    private ExoPlayer player;
    private PlayerView playerView;

    // Views
    private FrameLayout controlsOverlay, gestureOverlay, volumeBrightnessOverlay;
    private View topBar, bottomBar;
    private ImageButton btnBack, btnPip, btnMoreOptions, btnPrevious, btnRewind,
        btnPlayPause, btnForward, btnNext, btnShuffle, btnRepeat, btnLock, btnSubtitle,
        btnSpeed, btnFullscreen;
    private TextView tvPlayerTitle, tvSleepTimer, tvCurrentTime, tvTotalTime, seekAnimationText;
    private ImageView ivLockCenter, ivVbIcon;
    private TextView tvVbPercentage;
    private SeekBar seekbarPlayer;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private Runnable hideControlsRunnable;
    private Runnable sleepTimerRunnable;
    private GestureDetector gestureDetector;
    private AudioManager audioManager;

    private boolean isControlsVisible = true;
    private boolean isLocked = false;
    private boolean shuffleEnabled = false;
    private boolean repeatEnabled = false;
    private boolean sleepEndOfVideo = false;
    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private float playbackSpeed = 1f;

    private float startBrightness = -1f;
    private float startVolume = -1f;
    private float swipeStartY = -1f;
    private int swipeType = 0;

    private String videoPath;
    private String videoTitle;
    private long savedResumePosition = 0;
    private long lastHistorySaveTime = 0;

    private ArrayList<String> videoList;
    private int videoIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        bindViews();
        setupPlayer();
        setupGestures();
        setupControls();
        loadVideo();
    }

    private void bindViews() {
        playerView = findViewById(R.id.player_view);
        controlsOverlay = findViewById(R.id.controls_overlay);
        gestureOverlay = findViewById(R.id.gesture_overlay);
        volumeBrightnessOverlay = findViewById(R.id.volume_brightness_overlay);
        topBar = findViewById(R.id.top_bar);
        bottomBar = findViewById(R.id.bottom_bar);

        btnBack = findViewById(R.id.btn_back);
        btnPip = findViewById(R.id.btn_pip);
        btnMoreOptions = findViewById(R.id.btn_more_options);
        btnPrevious = findViewById(R.id.btn_previous);
        btnRewind = findViewById(R.id.btn_rewind);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnForward = findViewById(R.id.btn_forward);
        btnNext = findViewById(R.id.btn_next);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnLock = findViewById(R.id.btn_lock);
        btnSubtitle = findViewById(R.id.btn_subtitle);
        btnSpeed = findViewById(R.id.btn_speed);
        btnFullscreen = findViewById(R.id.btn_fullscreen);

        tvPlayerTitle = findViewById(R.id.tv_player_title);
        tvSleepTimer = findViewById(R.id.tv_sleep_timer);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        seekAnimationText = findViewById(R.id.seek_animation_text);
        ivLockCenter = findViewById(R.id.iv_lock_center);
        ivVbIcon = findViewById(R.id.iv_vb_icon);
        tvVbPercentage = findViewById(R.id.tv_vb_percentage);
        seekbarPlayer = findViewById(R.id.seekbar_player);
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);
        playerView.setResizeMode(currentResizeMode);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    updateDuration();
                    startProgressUpdate();
                    saveToHistory();
                } else if (state == Player.STATE_ENDED) {
                    handleVideoEnded();
                }
                updatePlayPauseButton();
            }

            @Override
            public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                // Reserved for future resolution badge use.
            }
        });
    }

    // ── Gestures ─────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isLocked) { showUnlockHint(); return true; }
                toggleControls();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isLocked) return true;
                float x = e.getX();
                int w = gestureOverlay.getWidth();
                if (x < w / 2f) {
                    seekRelative(-SEEK_MS);
                    showSeekAnimation(true, "-10s");
                } else {
                    seekRelative(SEEK_MS);
                    showSeekAnimation(false, "+10s");
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (isLocked) return;
                cycleResizeMode();
            }
        });

        gestureOverlay.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            handleSwipeGesture(event);
            return true;
        });
    }

    private void handleSwipeGesture(MotionEvent event) {
        if (isLocked) return;
        float screenWidth = gestureOverlay.getWidth();
        float screenHeight = gestureOverlay.getHeight();
        if (screenHeight <= 0) return;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                swipeStartY = event.getY();
                swipeType = event.getX() < screenWidth / 2f ? SWIPE_BRIGHTNESS : SWIPE_VOLUME;
                if (swipeType == SWIPE_BRIGHTNESS) {
                    startBrightness = getWindow().getAttributes().screenBrightness;
                    if (startBrightness < 0) {
                        try {
                            startBrightness = Settings.System.getInt(getContentResolver(),
                                Settings.System.SCREEN_BRIGHTNESS, 128) / 255f;
                        } catch (Exception e) { startBrightness = 0.5f; }
                    }
                } else {
                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (swipeStartY < 0) break;
                float dy = (swipeStartY - event.getY()) / screenHeight;
                if (Math.abs(event.getY() - swipeStartY) < 8) break;

                if (swipeType == SWIPE_BRIGHTNESS) {
                    float newBrightness = Math.max(0.01f, Math.min(1f, startBrightness + dy));
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = newBrightness;
                    getWindow().setAttributes(lp);
                    showVolumeBrightnessOverlay(true, (int) (newBrightness * 100));
                } else {
                    int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int newVol = Math.max(0, Math.min(maxVol, (int) (startVolume + dy * maxVol)));
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                    int pct = maxVol > 0 ? (int) ((float) newVol / maxVol * 100) : 0;
                    showVolumeBrightnessOverlay(false, pct);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                swipeStartY = -1f;
                handler.postDelayed(() -> {
                    if (volumeBrightnessOverlay != null) {
                        volumeBrightnessOverlay.setVisibility(View.GONE);
                    }
                }, 900);
                break;
        }
    }

    private void showVolumeBrightnessOverlay(boolean brightness, int percentage) {
        if (volumeBrightnessOverlay == null) return;
        volumeBrightnessOverlay.setVisibility(View.VISIBLE);
        ivVbIcon.setImageResource(brightness ? R.drawable.ic_brightness : R.drawable.ic_volume);
        tvVbPercentage.setText(percentage + "%");
    }

    private void cycleResizeMode() {
        if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
            showToast(getString(R.string.fill_screen));
        } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH;
            showToast(getString(R.string.crop_screen));
        } else {
            currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
            showToast(getString(R.string.fit_screen));
        }
        playerView.setResizeMode(currentResizeMode);
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    private void setupControls() {
        btnBack.setOnClickListener(v -> handleBackAction());
        btnPip.setOnClickListener(v -> enterPiPMode());
        btnMoreOptions.setOnClickListener(this::showMoreOptionsMenu);

        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) player.pause(); else player.play();
        });
        btnRewind.setOnClickListener(v -> { seekRelative(-SEEK_MS); showSeekAnimation(true, "-10s"); });
        btnForward.setOnClickListener(v -> { seekRelative(SEEK_MS); showSeekAnimation(false, "+10s"); });

        btnPrevious.setOnClickListener(v -> playAdjacent(-1));
        btnNext.setOnClickListener(v -> playAdjacent(1));
        updateAdjacentButtonsState();

        btnShuffle.setOnClickListener(v -> {
            shuffleEnabled = !shuffleEnabled;
            btnShuffle.setColorFilter(shuffleEnabled
                ? getColor(R.color.player_accent) : getColor(R.color.white));
        });
        btnRepeat.setOnClickListener(v -> {
            repeatEnabled = !repeatEnabled;
            player.setRepeatMode(repeatEnabled ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
            btnRepeat.setColorFilter(repeatEnabled
                ? getColor(R.color.player_accent) : getColor(R.color.white));
        });
        btnLock.setOnClickListener(v -> toggleLock());
        btnSubtitle.setOnClickListener(v ->
            showToast(getString(R.string.subtitles_unavailable)));
        btnSpeed.setOnClickListener(v -> showSpeedSheet());
        btnFullscreen.setOnClickListener(v -> toggleOrientation());

        seekbarPlayer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(hideControlsRunnable);
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                scheduleHideControls();
            }
        });
    }

    private void handleBackAction() {
        if (player != null && player.isPlaying()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPiPMode();
        } else {
            finish();
        }
    }

    private void showMoreOptionsMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(getString(R.string.sleep_timer));
        menu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().equals(getString(R.string.sleep_timer))) {
                showSleepTimerSheet();
            }
            return true;
        });
        menu.show();
    }

    private void playAdjacent(int direction) {
        if (videoList == null || videoList.isEmpty() || videoIndex < 0) {
            showToast(getString(R.string.no_more_videos));
            return;
        }
        int newIndex = videoIndex + direction;
        if (shuffleEnabled) {
            newIndex = (int) (Math.random() * videoList.size());
        }
        if (newIndex < 0 || newIndex >= videoList.size()) {
            showToast(getString(R.string.no_more_videos));
            return;
        }
        videoIndex = newIndex;
        String next = videoList.get(videoIndex);
        try {
            videoPath = next;
            savedResumePosition = 0;
            Uri uri = Uri.parse(next);
            tvPlayerTitle.setText(uri.getLastPathSegment());
            player.setMediaItem(MediaItem.fromUri(uri));
            player.prepare();
            player.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAdjacentButtonsState() {
        boolean hasList = videoList != null && videoList.size() > 1;
        btnPrevious.setAlpha(hasList ? 1f : 0.35f);
        btnNext.setAlpha(hasList ? 1f : 0.35f);
    }

    // ── Speed bottom sheet ───────────────────────────────────────────────────

    private void showSpeedSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_speed, null);
        dialog.setContentView(sheet);

        int[] chipIds = {
            R.id.chip_speed_025, R.id.chip_speed_05, R.id.chip_speed_075, R.id.chip_speed_1,
            R.id.chip_speed_125, R.id.chip_speed_15, R.id.chip_speed_175, R.id.chip_speed_2
        };
        TextView[] chips = new TextView[chipIds.length];
        for (int i = 0; i < chipIds.length; i++) {
            chips[i] = sheet.findViewById(chipIds[i]);
        }
        for (int i = 0; i < chips.length; i++) {
            final float speed = SPEEDS[i];
            chips[i].setBackgroundResource(speed == playbackSpeed
                ? R.drawable.chip_bg_selected : R.drawable.chip_bg_normal);
            chips[i].setOnClickListener(v -> {
                playbackSpeed = speed;
                player.setPlaybackSpeed(speed);
                for (int j = 0; j < chips.length; j++) {
                    chips[j].setBackgroundResource(SPEEDS[j] == speed
                        ? R.drawable.chip_bg_selected : R.drawable.chip_bg_normal);
                }
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    // ── Sleep timer bottom sheet ─────────────────────────────────────────────

    private void showSleepTimerSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_sleep_timer, null);
        dialog.setContentView(sheet);

        setupSleepOption(sheet, R.id.sleep_off, dialog, 0, false);
        setupSleepOption(sheet, R.id.sleep_5, dialog, 5, false);
        setupSleepOption(sheet, R.id.sleep_10, dialog, 10, false);
        setupSleepOption(sheet, R.id.sleep_15, dialog, 15, false);
        setupSleepOption(sheet, R.id.sleep_30, dialog, 30, false);
        setupSleepOption(sheet, R.id.sleep_60, dialog, 60, false);
        setupSleepOption(sheet, R.id.sleep_end, dialog, 0, true);

        dialog.show();
    }

    private void setupSleepOption(View sheet, int viewId, BottomSheetDialog dialog,
                                  int minutes, boolean endOfVideo) {
        View option = sheet.findViewById(viewId);
        option.setOnClickListener(v -> {
            cancelSleepTimer();
            if (endOfVideo) {
                sleepEndOfVideo = true;
                tvSleepTimer.setVisibility(View.VISIBLE);
                tvSleepTimer.setText(getString(R.string.sleep_end_of_video));
                showToast(getString(R.string.sleep_timer_set));
            } else if (minutes > 0) {
                scheduleSleepTimer(minutes);
                showToast(getString(R.string.sleep_timer_set));
            } else {
                showToast(getString(R.string.sleep_timer_off));
            }
            dialog.dismiss();
        });
    }

    private void scheduleSleepTimer(int minutes) {
        cancelSleepTimer();
        final long endTime = System.currentTimeMillis() + minutes * 60_000L;
        sleepTimerRunnable = new Runnable() {
            @Override
            public void run() {
                long remaining = endTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    if (player != null) player.pause();
                    tvSleepTimer.setVisibility(View.GONE);
                    return;
                }
                long minutesLeft = remaining / 60000;
                long secondsLeft = (remaining / 1000) % 60;
                tvSleepTimer.setVisibility(View.VISIBLE);
                tvSleepTimer.setText(String.format(Locale.US, "%d:%02d", minutesLeft, secondsLeft));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(sleepTimerRunnable);
    }

    private void cancelSleepTimer() {
        sleepEndOfVideo = false;
        if (sleepTimerRunnable != null) {
            handler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
        if (tvSleepTimer != null) tvSleepTimer.setVisibility(View.GONE);
    }

    private void handleVideoEnded() {
        if (sleepEndOfVideo && player != null) {
            player.pause();
            sleepEndOfVideo = false;
        }
    }

    // ── Load / lifecycle ─────────────────────────────────────────────────────

    private void loadVideo() {
        try {
            Uri uri = getIntent().getData();
            videoPath = getIntent().getStringExtra(AppConstants.EXTRA_VIDEO_PATH);
            videoTitle = getIntent().getStringExtra(AppConstants.EXTRA_VIDEO_TITLE);
            savedResumePosition = getIntent().getLongExtra(AppConstants.EXTRA_RESUME_POSITION, 0);
            videoList = getIntent().getStringArrayListExtra("extra_video_list");
            videoIndex = getIntent().getIntExtra("extra_video_index", -1);
            updateAdjacentButtonsState();

            if (uri == null && videoPath != null) {
                if (videoPath.startsWith("content://") || videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
                    uri = Uri.parse(videoPath);
                } else {
                    uri = android.net.Uri.fromFile(new java.io.File(videoPath));
                }
            }
            if (uri == null) { finish(); return; }
            if (videoPath == null) videoPath = uri.toString();

            tvPlayerTitle.setText(videoTitle != null ? videoTitle : uri.getLastPathSegment());
            player.setMediaItem(MediaItem.fromUri(uri));
            player.prepare();
            player.play();

            if (savedResumePosition <= 5000) {
                loadResumeFromHistory();
            } else {
                showResumeSnackbar(savedResumePosition);
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    private void loadResumeFromHistory() {
        if (videoPath == null) return;
        final String path = videoPath;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                HistoryEntity entity = db.historyDao().getByPath(path);
                if (entity != null && entity.resumePosition > 5000) {
                    handler.post(() -> showResumeSnackbar(entity.resumePosition));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void showResumeSnackbar(long resumePos) {
        handler.postDelayed(() -> {
            if (player == null || controlsOverlay == null) return;
            Snackbar.make(controlsOverlay,
                    getString(R.string.resume_playback, formatTime(resumePos)),
                    Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.yes), v -> player.seekTo(resumePos))
                .show();
        }, 800);
    }

    // ── Controls visibility ──────────────────────────────────────────────────

    private void toggleControls() {
        if (isControlsVisible) hideControls(); else showControls();
    }

    private void showControls() {
        isControlsVisible = true;
        topBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        topBar.animate().alpha(1f).setDuration(300).start();
        bottomBar.animate().alpha(1f).setDuration(300).start();
        scheduleHideControls();
    }

    private void hideControls() {
        isControlsVisible = false;
        topBar.animate().alpha(0f).setDuration(300)
            .withEndAction(() -> topBar.setVisibility(View.GONE)).start();
        bottomBar.animate().alpha(0f).setDuration(300)
            .withEndAction(() -> bottomBar.setVisibility(View.GONE)).start();
    }

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable);
        hideControlsRunnable = () -> { if (player != null && player.isPlaying()) hideControls(); };
        handler.postDelayed(hideControlsRunnable, HIDE_CONTROLS_DELAY);
    }

    // ── Lock ──────────────────────────────────────────────────────────────────

    private void toggleLock() {
        isLocked = !isLocked;
        btnLock.setImageResource(isLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
        ivLockCenter.setVisibility(isLocked ? View.VISIBLE : View.GONE);
        if (isLocked) {
            hideControls();
            showToast(getString(R.string.screen_locked));
        } else {
            showToast(getString(R.string.screen_unlocked));
        }
        ivLockCenter.setOnClickListener(v -> {
            if (isLocked) toggleLock();
        });
    }

    private void showUnlockHint() {
        showToast(getString(R.string.swipe_to_unlock));
    }

    // ── Seek / progress ───────────────────────────────────────────────────────

    private void seekRelative(long ms) {
        if (player == null) return;
        long pos = Math.max(0, player.getCurrentPosition() + ms);
        player.seekTo(pos);
    }

    private void showSeekAnimation(boolean left, String label) {
        if (seekAnimationText == null) return;
        seekAnimationText.setText(label);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) seekAnimationText.getLayoutParams();
        lp.gravity = left
            ? (android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START)
            : (android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);
        seekAnimationText.setLayoutParams(lp);
        seekAnimationText.setAlpha(1f);
        seekAnimationText.setVisibility(View.VISIBLE);
        seekAnimationText.animate().alpha(0f).setDuration(800)
            .withEndAction(() -> seekAnimationText.setVisibility(View.GONE)).start();
    }

    private void updatePlayPauseButton() {
        if (player == null || btnPlayPause == null) return;
        btnPlayPause.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void updateDuration() {
        if (player == null || seekbarPlayer == null) return;
        long dur = player.getDuration();
        if (dur > 0) {
            seekbarPlayer.setMax((int) dur);
            tvTotalTime.setText(formatTime(dur));
        }
    }

    private void startProgressUpdate() {
        progressRunnable = () -> {
            if (player != null && seekbarPlayer != null && player.isPlaying()) {
                long pos = player.getCurrentPosition();
                seekbarPlayer.setProgress((int) pos);
                tvCurrentTime.setText(formatTime(pos));

                long now = System.currentTimeMillis();
                if (now - lastHistorySaveTime >= HISTORY_SAVE_INTERVAL) {
                    lastHistorySaveTime = now;
                    saveResumePosition();
                }
            }
            if (handler != null && progressRunnable != null) {
                handler.postDelayed(progressRunnable, 500);
            }
        };
        handler.post(progressRunnable);
    }

    private void toggleOrientation() {
        int current = getRequestedOrientation();
        if (current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    // ── PiP ───────────────────────────────────────────────────────────────────

    private void enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
                VideoSize vs = player.getVideoSize();
                if (vs.width > 0 && vs.height > 0) {
                    builder.setAspectRatio(new Rational(vs.width, vs.height));
                }
                enterPictureInPictureMode(builder.build());
            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              @NonNull Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (controlsOverlay == null) return;
        if (isInPictureInPictureMode) {
            controlsOverlay.setVisibility(View.GONE);
        } else {
            controlsOverlay.setVisibility(View.VISIBLE);
            showControls();
        }
    }

    // ── History ───────────────────────────────────────────────────────────────

    private void saveToHistory() {
        if (videoPath == null) return;
        final String path = videoPath;
        final String title = videoTitle;
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                HistoryEntity existing = db.historyDao().getByPath(path);
                HistoryEntity entity = existing != null ? existing : new HistoryEntity();
                entity.videoPath = path;
                entity.videoTitle = title != null ? title : path;
                entity.lastWatched = System.currentTimeMillis();
                entity.watchCount = (existing != null ? existing.watchCount : 0) + 1;
                if (player != null) entity.videoDuration = player.getDuration();
                db.historyDao().insert(entity);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void saveResumePosition() {
        if (videoPath == null || player == null) return;
        final String path = videoPath;
        final String title = videoTitle;
        final long pos = player.getCurrentPosition();
        final long dur = player.getDuration();
        if (dur > 0 && pos > 0 && (dur - pos) > 5000) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(this);
                    HistoryEntity entity = db.historyDao().getByPath(path);
                    if (entity == null) {
                        entity = new HistoryEntity();
                        entity.videoPath = path;
                        entity.videoTitle = title != null ? title : path;
                        entity.lastWatched = System.currentTimeMillis();
                    }
                    entity.resumePosition = pos;
                    entity.videoDuration = dur;
                    db.historyDao().insert(entity);
                } catch (Exception e) { e.printStackTrace(); }
            });
        } else if (dur > 0 && (dur - pos) <= 5000) {
            clearResumePosition(path, title);
        }
    }

    private void clearResumePosition(String path, String title) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                HistoryEntity entity = db.historyDao().getByPath(path);
                if (entity != null) {
                    entity.resumePosition = 0;
                    db.historyDao().insert(entity);
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatTime(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, sec);
        return String.format(Locale.US, "%02d:%02d", m, sec);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            saveResumePosition();
            if (!isCurrentlyInPiP()) player.pause();
        }
    }

    @SuppressLint("NewApi")
    private boolean isCurrentlyInPiP() {
        try {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelSleepTimer();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (player != null) { player.release(); player = null; }
    }
}
