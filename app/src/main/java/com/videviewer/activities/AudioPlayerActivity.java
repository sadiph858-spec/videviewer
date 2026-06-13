package com.videviewer.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.videviewer.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * AudioPlayerActivity — Browse device audio files and play them with ExoPlayer.
 * Dark music-player UI: album art placeholder, seek bar, controls, song list.
 */
@UnstableApi
public class AudioPlayerActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 101;

    private ExoPlayer player;
    private final List<AudioFile> audioFiles = new ArrayList<>();
    private int currentIndex = -1;

    private TextView tvSongTitle, tvArtist, tvPosition, tvDuration, tvSongCount;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnRepeat, btnShuffle;
    private RecyclerView recyclerView;
    private AudioAdapter adapter;
    private View playerCard;

    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private boolean isRepeat = false;
    private boolean isShuffle = false;
    private boolean isSeeking = false;

    private final Runnable seekRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && !isSeeking) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur > 0) {
                    tvPosition.setText(formatTime(pos));
                    tvDuration.setText(formatTime(dur));
                    seekBar.setMax((int) dur);
                    seekBar.setProgress((int) pos);
                }
            }
            seekHandler.postDelayed(this, 500);
        }
    };

    // ──────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        Toolbar toolbar = findViewById(R.id.toolbar_audio);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.audio_player));
        }

        bindViews();
        setupPlayer();
        setupControls();
        setupRecycler();
        requestPermissionAndLoad();
    }

    @Override
    protected void onStart() {
        super.onStart();
        seekHandler.post(seekRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        seekHandler.removeCallbacks(seekRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ──────────────────────────────────────────────────
    // Setup
    // ──────────────────────────────────────────────────

    private void bindViews() {
        tvSongTitle  = findViewById(R.id.tv_audio_title);
        tvArtist     = findViewById(R.id.tv_audio_artist);
        tvPosition   = findViewById(R.id.tv_audio_position);
        tvDuration   = findViewById(R.id.tv_audio_duration);
        tvSongCount  = findViewById(R.id.tv_audio_song_count);
        seekBar      = findViewById(R.id.seek_audio);
        btnPlayPause = findViewById(R.id.btn_audio_play_pause);
        btnPrev      = findViewById(R.id.btn_audio_prev);
        btnNext      = findViewById(R.id.btn_audio_next);
        btnRepeat    = findViewById(R.id.btn_audio_repeat);
        btnShuffle   = findViewById(R.id.btn_audio_shuffle);
        recyclerView = findViewById(R.id.rv_audio_list);
        playerCard   = findViewById(R.id.card_now_playing);
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(
                    isPlaying ? android.R.drawable.ic_media_pause
                              : android.R.drawable.ic_media_play);
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    if (isRepeat) {
                        player.seekTo(0);
                        player.play();
                    } else {
                        playNext();
                    }
                }
            }
        });
    }

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) player.pause();
            else player.play();
        });

        btnPrev.setOnClickListener(v -> playPrev());
        btnNext.setOnClickListener(v -> playNext());

        btnRepeat.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            btnRepeat.setAlpha(isRepeat ? 1f : 0.45f);
            Toast.makeText(this,
                isRepeat ? "Repeat ON" : "Repeat OFF",
                Toast.LENGTH_SHORT).show();
        });
        btnRepeat.setAlpha(0.45f);

        btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            btnShuffle.setAlpha(isShuffle ? 1f : 0.45f);
            Toast.makeText(this,
                isShuffle ? "Shuffle ON" : "Shuffle OFF",
                Toast.LENGTH_SHORT).show();
        });
        btnShuffle.setAlpha(0.45f);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser) tvPosition.setText(formatTime(p));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                if (player != null) player.seekTo(sb.getProgress());
                isSeeking = false;
            }
        });
    }

    private void setupRecycler() {
        adapter = new AudioAdapter(audioFiles, this::playSong);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
            new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);
    }

    // ──────────────────────────────────────────────────
    // Permissions
    // ──────────────────────────────────────────────────

    private void requestPermissionAndLoad() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_AUDIO
            : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            loadAudioFiles();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{perm}, PERM_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PERM_REQUEST && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadAudioFiles();
        } else {
            Toast.makeText(this,
                "Storage permission required to browse audio files",
                Toast.LENGTH_LONG).show();
        }
    }

    // ──────────────────────────────────────────────────
    // MediaStore scan
    // ──────────────────────────────────────────────────

    private void loadAudioFiles() {
        new Thread(() -> {
            List<AudioFile> list = new ArrayList<>();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] cols = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            };
            String sel = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            String order = MediaStore.Audio.Media.TITLE + " ASC";

            try (Cursor c = getContentResolver().query(uri, cols, sel, null, order)) {
                if (c != null) {
                    int idCol       = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int albumCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                    int durCol      = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    int dataCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                    while (c.moveToNext()) {
                        long id       = c.getLong(idCol);
                        String title  = c.getString(titleCol);
                        String artist = c.getString(artistCol);
                        String album  = c.getString(albumCol);
                        long dur      = c.getLong(durCol);
                        String path   = c.getString(dataCol);

                        list.add(new AudioFile(id, title, artist, album, dur, path));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                audioFiles.clear();
                audioFiles.addAll(list);
                adapter.notifyDataSetChanged();
                tvSongCount.setText(list.size() + " songs");
                if (list.isEmpty()) {
                    Toast.makeText(this,
                        "No audio files found on this device",
                        Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ──────────────────────────────────────────────────
    // Playback
    // ──────────────────────────────────────────────────

    private void playSong(int index) {
        if (index < 0 || index >= audioFiles.size()) return;
        currentIndex = index;
        AudioFile af = audioFiles.get(index);

        Uri contentUri = Uri.withAppendedPath(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            String.valueOf(af.id));

        player.setMediaItem(MediaItem.fromUri(contentUri));
        player.prepare();
        player.play();

        tvSongTitle.setText(af.title);
        tvArtist.setText(
            af.artist == null || af.artist.equals("<unknown>") ? af.album : af.artist);
        playerCard.setVisibility(View.VISIBLE);
        adapter.setCurrentIndex(index);
        recyclerView.scrollToPosition(index);
    }

    private void playNext() {
        if (audioFiles.isEmpty()) return;
        int next;
        if (isShuffle) {
            next = (int) (Math.random() * audioFiles.size());
        } else {
            next = (currentIndex + 1) % audioFiles.size();
        }
        playSong(next);
    }

    private void playPrev() {
        if (audioFiles.isEmpty()) return;
        int prev = currentIndex <= 0 ? audioFiles.size() - 1 : currentIndex - 1;
        playSong(prev);
    }

    // ──────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────

    private String formatTime(long ms) {
        long secs = TimeUnit.MILLISECONDS.toSeconds(ms);
        long mins = secs / 60;
        long hrs  = mins / 60;
        if (hrs > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hrs, mins % 60, secs % 60);
        }
        return String.format(Locale.US, "%d:%02d", mins, secs % 60);
    }

    // ──────────────────────────────────────────────────
    // Data model
    // ──────────────────────────────────────────────────

    static class AudioFile {
        final long id;
        final String title, artist, album, path;
        final long duration;

        AudioFile(long id, String title, String artist, String album,
                  long duration, String path) {
            this.id       = id;
            this.title    = title != null ? title : "Unknown";
            this.artist   = artist;
            this.album    = album != null ? album : "";
            this.duration = duration;
            this.path     = path;
        }
    }

    // ──────────────────────────────────────────────────
    // RecyclerView Adapter
    // ──────────────────────────────────────────────────

    static class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.VH> {

        interface OnSongClick { void play(int index); }

        private final List<AudioFile> list;
        private final OnSongClick callback;
        private int currentIndex = -1;

        AudioAdapter(List<AudioFile> list, OnSongClick cb) {
            this.list = list;
            this.callback = cb;
        }

        void setCurrentIndex(int i) {
            int old = currentIndex;
            currentIndex = i;
            if (old >= 0) notifyItemChanged(old);
            notifyItemChanged(i);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audio_file, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AudioFile af = list.get(pos);
            h.tvTitle.setText(af.title);
            h.tvMeta.setText(
                (af.artist == null || af.artist.equals("<unknown>") ? "" : af.artist + " · ")
                + formatMs(af.duration));
            boolean active = pos == currentIndex;
            h.tvTitle.setTextColor(active
                ? 0xFFBB86FC   // purple accent when active
                : 0xDE000000); // default
            h.itemView.setOnClickListener(v -> callback.play(pos));
        }

        @Override
        public int getItemCount() { return list.size(); }

        private String formatMs(long ms) {
            long s = ms / 1000, m = s / 60;
            return String.format(Locale.US, "%d:%02d", m, s % 60);
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMeta;
            VH(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_audio_item_title);
                tvMeta  = v.findViewById(R.id.tv_audio_item_meta);
            }
        }
    }
}
