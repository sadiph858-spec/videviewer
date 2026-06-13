package com.videviewer.activities;

  import android.annotation.SuppressLint;
  import android.app.PictureInPictureParams;
  import android.content.BroadcastReceiver;
  import android.content.Context;
  import android.content.Intent;
  import android.content.IntentFilter;
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
  import android.view.ScaleGestureDetector;
  import android.view.View;
  import android.view.WindowManager;
  import android.widget.ImageButton;
  import android.widget.ImageView;
  import android.widget.SeekBar;
  import android.widget.TextView;
  import android.widget.Toast;
  import androidx.annotation.NonNull;
  import androidx.appcompat.app.AppCompatActivity;
  import androidx.media3.common.MediaItem;
  import androidx.media3.common.Player;
  import androidx.media3.common.VideoSize;
  import androidx.media3.exoplayer.ExoPlayer;
  import androidx.media3.ui.PlayerView;
  import com.videviewer.R;
  import com.videviewer.database.AppDatabase;
  import com.videviewer.database.HistoryEntity;
  import com.videviewer.databinding.ActivityPlayerBinding;
  import com.videviewer.utils.AppConstants;
  import java.util.concurrent.Executors;

  public class PlayerActivity extends AppCompatActivity {

      private ActivityPlayerBinding binding;
      private ExoPlayer player;
      private Handler handler = new Handler(Looper.getMainLooper());
      private Runnable progressRunnable;
      private Runnable hideControlsRunnable;
      private GestureDetector gestureDetector;
      private ScaleGestureDetector scaleGestureDetector;
      private AudioManager audioManager;
      private boolean isControlsVisible = true;
      private boolean isLocked = false;
      private float scaleFactor = 1.0f;
      private float startBrightness = -1f;
      private float startVolume = -1f;
      private float swipeStartY = -1f;
      private int swipeType = 0; // 1=brightness, 2=volume

      private static final int SWIPE_BRIGHTNESS = 1;
      private static final int SWIPE_VOLUME = 2;
      private static final long SEEK_MS = 10000;
      private static final long HIDE_CONTROLS_DELAY = 3000;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          binding = ActivityPlayerBinding.inflate(getLayoutInflater());
          setContentView(binding.getRoot());
          getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

          audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
          setupPlayer();
          setupGestures();
          setupControls();
          loadVideo();
      }

      private void setupPlayer() {
          player = new ExoPlayer.Builder(this).build();
          binding.playerView.setPlayer(player);
          binding.playerView.setUseController(false);

          player.addListener(new Player.Listener() {
              @Override
              public void onPlaybackStateChanged(int state) {
                  if (state == Player.STATE_READY) {
                      updateDuration();
                      startProgressUpdate();
                      saveToHistory();
                  }
                  updatePlayPauseButton();
              }
              @Override
              public void onVideoSizeChanged(@NonNull VideoSize videoSize) {
                  updateResolutionBadge(videoSize);
              }
          });
      }

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
                  int w = binding.playerView.getWidth();
                  if (x < w / 2f) {
                      seekRelative(-SEEK_MS);
                      showSeekAnimation(binding.seekBackAnim, "-10s");
                  } else {
                      seekRelative(SEEK_MS);
                      showSeekAnimation(binding.seekFwdAnim, "+10s");
                  }
                  return true;
              }
          });

          scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
              @Override
              public boolean onScale(ScaleGestureDetector detector) {
                  scaleFactor *= detector.getScaleFactor();
                  scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 3.0f));
                  binding.playerView.setScaleX(scaleFactor);
                  binding.playerView.setScaleY(scaleFactor);
                  return true;
              }
          });

          binding.playerGestureOverlay.setOnTouchListener((v, event) -> {
              scaleGestureDetector.onTouchEvent(event);
              gestureDetector.onTouchEvent(event);
              handleSwipeGesture(event);
              return true;
          });
      }

      private void handleSwipeGesture(MotionEvent event) {
          if (isLocked) return;
          float screenWidth = binding.playerView.getWidth();
          switch (event.getAction()) {
              case MotionEvent.ACTION_DOWN:
                  swipeStartY = event.getY();
                  swipeType = event.getX() < screenWidth / 2f ? SWIPE_BRIGHTNESS : SWIPE_VOLUME;
                  if (swipeType == SWIPE_BRIGHTNESS) {
                      startBrightness = getWindow().getAttributes().screenBrightness;
                      if (startBrightness < 0) startBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128) / 255f;
                  } else {
                      startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                  }
                  break;
              case MotionEvent.ACTION_MOVE:
                  float dy = (swipeStartY - event.getY()) / binding.playerView.getHeight();
                  if (swipeType == SWIPE_BRIGHTNESS) {
                      float newBrightness = Math.max(0.01f, Math.min(1f, startBrightness + dy));
                      WindowManager.LayoutParams lp = getWindow().getAttributes();
                      lp.screenBrightness = newBrightness;
                      getWindow().setAttributes(lp);
                      int pct = (int)(newBrightness * 100);
                      binding.brightnessIndicator.setVisibility(View.VISIBLE);
                      binding.brightnessValue.setText(pct + "%");
                      binding.brightnessBar.setProgress(pct);
                  } else {
                      int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                      int newVol = Math.max(0, Math.min(maxVol, (int)(startVolume + dy * maxVol)));
                      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0);
                      int pct = (int)((float) newVol / maxVol * 100);
                      binding.volumeIndicator.setVisibility(View.VISIBLE);
                      binding.volumeValue.setText(pct + "%");
                      binding.volumeBar.setProgress(pct);
                  }
                  break;
              case MotionEvent.ACTION_UP:
              case MotionEvent.ACTION_CANCEL:
                  handler.postDelayed(() -> {
                      binding.brightnessIndicator.setVisibility(View.GONE);
                      binding.volumeIndicator.setVisibility(View.GONE);
                  }, 1200);
                  break;
          }
      }

      private void setupControls() {
          binding.btnBack.setOnClickListener(v -> onBackPressed());
          binding.btnPlayPause.setOnClickListener(v -> { if (player.isPlaying()) player.pause(); else player.play(); });
          binding.btnReplay.setOnClickListener(v -> { seekRelative(-SEEK_MS); showSeekAnimation(binding.seekBackAnim, "-10s"); });
          binding.btnForward.setOnClickListener(v -> { seekRelative(SEEK_MS); showSeekAnimation(binding.seekFwdAnim, "+10s"); });
          binding.btnLock.setOnClickListener(v -> toggleLock());
          binding.btnPip.setOnClickListener(v -> enterPiP());
          binding.btnRotate.setOnClickListener(v -> toggleRotation());
          binding.btnScreenshot.setOnClickListener(v -> takeScreenshot());

          binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
              @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                  if (fromUser) {
                      player.seekTo(progress);
                      binding.tvCurrentTime.setText(formatTime(progress));
                  }
              }
              @Override public void onStartTrackingTouch(SeekBar seekBar) { handler.removeCallbacks(hideControlsRunnable); }
              @Override public void onStopTrackingTouch(SeekBar seekBar) { scheduleHideControls(); }
          });
      }

      private void loadVideo() {
          Uri uri = getIntent().getData();
          String path = getIntent().getStringExtra(AppConstants.EXTRA_VIDEO_PATH);
          String title = getIntent().getStringExtra(AppConstants.EXTRA_VIDEO_TITLE);
          if (uri == null && path != null) uri = Uri.parse(path);
          if (uri == null) { finish(); return; }
          binding.tvVideoTitle.setText(title != null ? title : uri.getLastPathSegment());
          player.setMediaItem(MediaItem.fromUri(uri));
          player.prepare();
          player.play();
      }

      private void toggleControls() {
          if (isControlsVisible) hideControls();
          else showControls();
      }

      private void showControls() {
          isControlsVisible = true;
          binding.topBar.animate().alpha(1f).setDuration(250).withStartAction(() -> binding.topBar.setVisibility(View.VISIBLE)).start();
          binding.bottomBar.animate().alpha(1f).setDuration(250).withStartAction(() -> binding.bottomBar.setVisibility(View.VISIBLE)).start();
          scheduleHideControls();
      }

      private void hideControls() {
          isControlsVisible = false;
          binding.topBar.animate().alpha(0f).setDuration(250).withEndAction(() -> binding.topBar.setVisibility(View.GONE)).start();
          binding.bottomBar.animate().alpha(0f).setDuration(250).withEndAction(() -> binding.bottomBar.setVisibility(View.GONE)).start();
      }

      private void scheduleHideControls() {
          handler.removeCallbacks(hideControlsRunnable);
          hideControlsRunnable = () -> { if (player.isPlaying()) hideControls(); };
          handler.postDelayed(hideControlsRunnable, HIDE_CONTROLS_DELAY);
      }

      private void toggleLock() {
          isLocked = !isLocked;
          binding.btnLock.setImageResource(isLocked ? R.drawable.ic_lock_closed : R.drawable.ic_lock_open);
          binding.lockOverlay.setVisibility(isLocked ? View.VISIBLE : View.GONE);
          if (isLocked) { hideControls(); Toast.makeText(this, R.string.screen_locked, Toast.LENGTH_SHORT).show(); }
          else Toast.makeText(this, R.string.screen_unlocked, Toast.LENGTH_SHORT).show();
      }

      private void showUnlockHint() {
          Toast.makeText(this, R.string.swipe_to_unlock, Toast.LENGTH_SHORT).show();
      }

      private void seekRelative(long ms) {
          long pos = Math.max(0, player.getCurrentPosition() + ms);
          player.seekTo(pos);
      }

      private void showSeekAnimation(View anim, String label) {
          if (anim instanceof TextView) ((TextView) anim).setText(label);
          anim.setAlpha(1f);
          anim.setVisibility(View.VISIBLE);
          anim.animate().alpha(0f).setDuration(800).withEndAction(() -> anim.setVisibility(View.GONE)).start();
      }

      private void updatePlayPauseButton() {
          binding.btnPlayPause.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
      }

      private void updateDuration() {
          long dur = player.getDuration();
          binding.seekBar.setMax((int) dur);
          binding.tvTotalTime.setText(formatTime(dur));
      }

      private void startProgressUpdate() {
          progressRunnable = () -> {
              if (player.isPlaying()) {
                  long pos = player.getCurrentPosition();
                  binding.seekBar.setProgress((int) pos);
                  binding.tvCurrentTime.setText(formatTime(pos));
              }
              handler.postDelayed(progressRunnable, 500);
          };
          handler.post(progressRunnable);
      }

      private void updateResolutionBadge(VideoSize size) {
          String badge;
          if (size.height >= 2160) badge = "4K ULTRA";
          else if (size.height >= 1080) badge = "1080p FULL HD";
          else if (size.height >= 720) badge = "720p HD";
          else if (size.height >= 480) badge = "480p";
          else badge = size.height + "p";
          binding.tvResolution.setText(badge);
          binding.tvResolution.setVisibility(View.VISIBLE);
      }

      private void enterPiP() {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
              VideoSize vs = player.getVideoSize();
              if (vs.width > 0 && vs.height > 0) builder.setAspectRatio(new Rational(vs.width, vs.height));
              enterPictureInPictureMode(builder.build());
          }
      }

      private void toggleRotation() {
          int current = getRequestedOrientation();
          if (current == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
              setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
          else setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }

      private void takeScreenshot() {
          Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show();
      }

      private void saveToHistory() {
          String path = getIntent().getStringExtra(AppConstants.EXTRA_VIDEO_PATH);
          String title = getIntent().getStringExtra(AppConstants.EXTRA_VIDEO_TITLE);
          if (path == null) return;
          Executors.newSingleThreadExecutor().execute(() -> {
              HistoryEntity entity = new HistoryEntity();
              entity.videoPath = path;
              entity.videoTitle = title != null ? title : path;
              entity.lastWatched = System.currentTimeMillis();
              AppDatabase.getInstance(this).historyDao().insert(entity);
          });
      }

      private String formatTime(long ms) {
          long s = ms / 1000;
          long h = s / 3600;
          long m = (s % 3600) / 60;
          long sec = s % 60;
          if (h > 0) return String.format("%d:%02d:%02d", h, m, sec);
          return String.format("%02d:%02d", m, sec);
      }

      @Override
      protected void onPause() {
          super.onPause();
          if (player != null) player.pause();
      }

      @Override
      protected void onDestroy() {
          super.onDestroy();
          if (handler != null) { handler.removeCallbacksAndMessages(null); }
          if (player != null) { player.release(); player = null; }
      }

      @Override
      public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
          super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
          if (isInPictureInPictureMode) { binding.topBar.setVisibility(View.GONE); binding.bottomBar.setVisibility(View.GONE); }
          else showControls();
      }
  }