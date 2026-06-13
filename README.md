# 🎬 VidéViewer v3.0.0 — Premium Android Video Player

  <p align="center">
    <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
    <img src="https://img.shields.io/badge/Min_SDK-21_(Android_5)-blue?style=for-the-badge" />
    <img src="https://img.shields.io/badge/Version-3.0.0-purple?style=for-the-badge" />
    <img src="https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=java" />
  </p>

  ## ✨ What's New in v3.0.0

  ### 🎬 Redesigned Player
  - Full dark theme (`#0D0D0D` background)
  - **Single tap** — show/hide controls with smooth fade
  - **Double tap left** — seek back 10s (−10s animation)
  - **Double tap right** — seek forward 10s (+10s animation)
  - **Swipe left** — brightness control with visual indicator
  - **Swipe right** — volume control with visual indicator
  - **Pinch to zoom** — up to 3×
  - **Lock button** — lock screen, tap lock to unlock
  - Resolution badge: 4K ULTRA / 1080p FULL HD / 720p HD
  - Picture-in-Picture (PiP) support
  - Cast support via Chromecast

  ### 📱 5-Tab Bottom Navigation
  | Tab | Feature |
  |-----|---------|
  | **Videos** | Grid/list of all device videos with search, thumbnails, format badges |
  | **Browser** | Built-in WebView + automatic video detection on Dailymotion, Vimeo |
  | **Downloads** | Paste any direct URL (.mp4 .mkv .webm), pause/resume, speed indicator |
  | **Storage** | WhatsApp Status Saver + folder browser |
  | **More** | Private Vault, Settings, About |

  ### 🔒 Private Vault
  - PIN lock with shake animation on wrong PIN
  - Auto-lock when app goes to background
  - Move videos to encrypted private folder

  ### 📥 Download Manager
  - Download any direct video URL
  - Progress notifications with speed (MB/s)
  - Save to `Downloads/VidViewer/`
  - Foreground service with pause/resume

  ### 📊 WhatsApp Status Saver
  - Auto-scan WhatsApp & WhatsApp Business statuses
  - One-tap save to gallery
  - Shows both photos and videos

  ### 🐛 Bug Fixes
  - Fixed Videos tab MediaStore permission handling (Android 13+ compatible)
  - Fixed watch history saving with Room DB
  - Fixed share via FileProvider
  - Fixed Room database migrations

  ## 🔧 Supported Formats
  `MP4` `MKV` `AVI` `MOV` `3GP` `FLV` `M4V` `WMV` `RMVB` `TS` `MPEG`

  All formats powered by **ExoPlayer + NextLib FFmpeg extension**

  ## 🏗️ Tech Stack
  - **Player**: Media3 ExoPlayer 1.2.1 + NextLib FFmpeg ext
  - **Database**: Room 2.6.1
  - **Images**: Glide 4.16.0
  - **Ads**: Google AdMob
  - **Cast**: Google Cast Framework
  - **Network**: OkHttp 4.12.0
  - **Min SDK**: 21 (Android 5.0)
  - **Target SDK**: 34 (Android 14)

  ## 🚀 Building
  ```bash
  git clone https://github.com/sadiph858-spec/videviewer
  cd videviewer
  ./gradlew assembleRelease
  ```

  > **Note:** Replace `app/google-services.json` with your own Firebase config for AdMob to work properly.
  