package com.videviewer.activities;

import android.app.PictureInPictureParams;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.*;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.videviewer.R;
import com.videviewer.database.AppDatabase;
import com.videviewer.database.HistoryEntity;
import com.videviewer.utils.AppConstants;
import java.util.concurrent.Executors;

/**
 * PlayerActivity - Full-featured ExoPlayer video player
 * Supports gesture controls, PiP, speed control, sleep timer, resume playback
 */
@UnstableApi
public class PlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private SharedPreferences prefs;
    private AppDatabase db;

    private String videoPath;
    private String videoTitle;
    private long resumePosition = 0;
    private boolean isInPiP = false;
    private boolean playerInitialized = false;

    private CountDownTimer sleepTimer;

    private GestureDetector gestureDetector;

    private TextView tvTitle, tvSpeed, tvSleepTimer;
    private ImageButton btnSpeed, btnPip, btnSleep, btnSubtitle;
    private View controlsOverlay;
    private Handler hideControlsHandler = new Handler(Looper.getMainLooper());
    private static final long CONTROLS_HIDE_DELAY = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.activity_player);

            prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
            db = AppDatabase.getInstance(this);

            videoPath = getIntent().getStringExtra(AppConstants.EXTRA_VIDEO_PATH);
            videoTitle = getIntent().getStringExtra("video_title");
            resumePosition = getIntent().getLongExtra("resume_position", 0);

            if (videoPath == null || videoPath.isEmpty()) {
                Toast.makeText(this, "No video to play", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            initViews();
            initPlayer();
            setupGestures();
            setupControls();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error starting player", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        try {
            playerView = findViewById(R.id.player_view);
            tvTitle = findViewById(R.id.tv_player_title);
            tvSpeed = findViewById(R.id.tv_speed_indicator);
            tvSleepTimer = findViewById(R.id.tv_sleep_timer);
            btnSpeed = findViewById(R.id.btn_playback_speed);
            btnPip = findViewById(R.id.btn_pip);
            btnSleep = findViewById(R.id.btn_sleep_timer);
            btnSubtitle = findViewById(R.id.btn_subtitle);
            controlsOverlay = findViewById(R.id.controls_overlay);

            if (tvTitle != null && videoTitle != null) tvTitle.setText(videoTitle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPlayer() {
        try {
            player = new ExoPlayer.Builder(this).build();

            if (playerView == null) {
                finish();
                return;
            }
            playerView.setPlayer(player);
            playerView.setKeepScreenOn(true);

            // Build URI - prefer content:// URI for reliability on all API levels
            Uri videoUri;
            if (videoPath.startsWith("content://") || videoPath.startsWith("file://")) {
                videoUri = Uri.parse(videoPath);
            } else {
                // Try as file path
                try {
                    videoUri = Uri.parse("file://" + videoPath);
                } catch (Exception e) {
                    videoUri = Uri.parse(videoPath);
                }
            }

            MediaItem mediaItem = new MediaItem.Builder()
                .setUri(videoUri)
                .build();

            player.setMediaItem(mediaItem);

            // Apply saved playback speed
            float speed = prefs.getFloat(AppConstants.PREF_PLAYBACK_SPEED, 1.0f);
            player.setPlaybackSpeed(speed);
            if (tvSpeed != null) tvSpeed.setText(speed + "x");

            // Apply repeat mode
            int repeatMode = prefs.getInt(AppConstants.PREF_REPEAT_MODE, Player.REPEAT_MODE_OFF);
            player.setRepeatMode(repeatMode);

            // Resume position
            boolean resumeEnabled = prefs.getBoolean(AppConstants.PREF_RESUME_PLAYBACK, true);
            if (resumeEnabled && resumePosition > 0) {
                player.seekTo(resumePosition);
            }

            // Player event listener for error handling and history
            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Toast.makeText(PlayerActivity.this,
                        "Playback error: " + error.getErrorCodeName(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        playerInitialized = true;
                        saveToHistory();
                    }
                }
            });

            player.prepare();
            player.setPlayWhenReady(true);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to initialize player", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupGestures() {
        try {
            gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    toggleControlsVisibility();
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (player == null) return false;
                    try {
                        float screenWidth = playerView != null ? playerView.getWidth() : 1;
                        if (e.getX() < screenWidth / 2) {
                            player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
                            showToast("-10s");
                        } else {
                            long newPos = player.getCurrentPosition() + 10000;
                            long duration = player.getDuration();
                            if (duration > 0) newPos = Math.min(newPos, duration);
                            player.seekTo(newPos);
                            showToast("+10s");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return true;
                }
            });

            if (playerView != null) {
                playerView.setOnTouchListener((v, event) -> {
                    if (gestureDetector != null) gestureDetector.onTouchEvent(event);
                    return true;
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupControls() {
        try {
            if (btnSpeed != null) btnSpeed.setOnClickListener(v -> showSpeedDialog());

            if (btnPip != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && getPackageManager().hasSystemFeature(
                            PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    btnPip.setVisibility(View.VISIBLE);
                    btnPip.setOnClickListener(v -> {
                        try { enterPiPMode(); } catch (Exception e) { e.printStackTrace(); }
                    });
                } else {
                    btnPip.setVisibility(View.GONE);
                }
            }

            if (btnSleep != null) btnSleep.setOnClickListener(v -> showSleepTimerDialog());

            if (btnSubtitle != null) {
                btnSubtitle.setOnClickListener(v ->
                    showToast(getString(R.string.subtitle_not_loaded)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSpeedDialog() {
        try {
            if (player == null) return;
            String[] speedLabels = {"0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x"};
            float currentSpeed = player.getPlaybackParameters().speed;
            int currentIdx = 3;
            for (int i = 0; i < AppConstants.PLAYBACK_SPEEDS.length; i++) {
                if (Math.abs(AppConstants.PLAYBACK_SPEEDS[i] - currentSpeed) < 0.01f) {
                    currentIdx = i;
                    break;
                }
            }
            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(R.string.playback_speed)
                .setSingleChoiceItems(speedLabels, currentIdx, (dialog, which) -> {
                    try {
                        float speed = AppConstants.PLAYBACK_SPEEDS[which];
                        player.setPlaybackSpeed(speed);
                        if (tvSpeed != null) tvSpeed.setText(speedLabels[which]);
                        prefs.edit().putFloat(AppConstants.PREF_PLAYBACK_SPEED, speed).apply();
                    } catch (Exception e) { e.printStackTrace(); }
                    dialog.dismiss();
                })
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSleepTimerDialog() {
        try {
            String[] options = {
                getString(R.string.sleep_off),
                "5 min", "10 min", "15 min", "30 min", "45 min", "60 min"
            };
            long[] durations = {0, 5, 10, 15, 30, 45, 60};

            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(R.string.sleep_timer)
                .setItems(options, (dialog, which) -> {
                    try {
                        if (sleepTimer != null) sleepTimer.cancel();
                        long ms = durations[which] * 60 * 1000;
                        if (ms > 0) {
                            sleepTimer = new CountDownTimer(ms, 1000) {
                                @Override
                                public void onTick(long ms) {
                                    long mins = ms / 60000;
                                    long secs = (ms % 60000) / 1000;
                                    if (tvSleepTimer != null) {
                                        tvSleepTimer.setVisibility(View.VISIBLE);
                                        tvSleepTimer.setText(
                                            String.format("%02d:%02d", mins, secs));
                                    }
                                }
                                @Override
                                public void onFinish() {
                                    try {
                                        if (player != null) player.pause();
                                        if (tvSleepTimer != null)
                                            tvSleepTimer.setVisibility(View.GONE);
                                    } catch (Exception e) { e.printStackTrace(); }
                                }
                            }.start();
                        } else {
                            if (tvSleepTimer != null) tvSleepTimer.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enterPiPMode() {
        try {
            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
            builder.setAspectRatio(new Rational(16, 9));
            enterPictureInPictureMode(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                               android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        isInPiP = isInPictureInPictureMode;
        if (controlsOverlay != null) {
            controlsOverlay.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        }
    }

    private void toggleControlsVisibility() {
        try {
            if (controlsOverlay == null) return;
            hideControlsHandler.removeCallbacksAndMessages(null);
            if (controlsOverlay.getVisibility() == View.VISIBLE) {
                controlsOverlay.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> {
                        if (controlsOverlay != null) controlsOverlay.setVisibility(View.GONE);
                    }).start();
            } else {
                controlsOverlay.setVisibility(View.VISIBLE);
                controlsOverlay.animate().alpha(1f).setDuration(300).start();
                hideControlsHandler.postDelayed(() -> {
                    if (controlsOverlay != null) {
                        controlsOverlay.animate().alpha(0f).setDuration(300)
                            .withEndAction(() -> {
                                if (controlsOverlay != null)
                                    controlsOverlay.setVisibility(View.GONE);
                            }).start();
                    }
                }, CONTROLS_HIDE_DELAY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveToHistory() {
        if (videoPath == null) return;
        final String path = videoPath;
        final String title = videoTitle != null ? videoTitle : "Unknown";
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                HistoryEntity entity = new HistoryEntity();
                entity.videoPath = path;
                entity.videoTitle = title;
                entity.lastWatched = System.currentTimeMillis();
                entity.resumePosition = 0;
                if (player != null) {
                    entity.videoDuration = player.getDuration();
                }
                db.historyDao().insert(entity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void saveResumePosition() {
        try {
            if (player != null && videoPath != null && playerInitialized) {
                long pos = player.getCurrentPosition();
                long now = System.currentTimeMillis();
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        db.historyDao().updateResumePosition(videoPath, pos, now);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showToast(String msg) {
        try {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (!isInPiP && player != null) {
                saveResumePosition();
                player.pause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (player != null && !isInPiP) {
                player.setPlayWhenReady(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (sleepTimer != null) sleepTimer.cancel();
            hideControlsHandler.removeCallbacksAndMessages(null);
            if (player != null) {
                player.release();
                player = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
