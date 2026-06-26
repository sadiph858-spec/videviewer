# 🎬 VidéViewer - Premium Android Video Player

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Min_SDK-26_(Android_8)-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=java" />
  <img src="https://img.shields.io/badge/Player-ExoPlayer_Media3-purple?style=for-the-badge" />
  <img src="https://img.shields.io/badge/UI-Material_Design_3-teal?style=for-the-badge" />
</p>

<p align="center">
  <img src="https://github.com/sadiph858-spec/videviewer/actions/workflows/build-apk.yml/badge.svg" />
</p>

---

## 📥 Download APK

➡️ **[Releases পেজ থেকে APK ডাউনলোড করুন](../../releases/latest)**

অথবা **Actions** ট্যাব থেকে latest build artifact ডাউনলোড করুন।

---

## ✨ Features

### 🎥 Video Playback
- ExoPlayer (Media3) powered full-screen player
- Gesture controls (swipe for brightness/volume)
- Double-tap to seek ±10 seconds
- Playback speed control (0.25x – 2x)
- Picture-in-Picture (PiP) mode
- Subtitle support
- Sleep timer
- Repeat & Shuffle modes
- Resume playback

### 📁 Library Management
- Auto-scan all device videos
- Grid View & List View
- Folder browser
- Search videos
- Sort by name, date, size, duration
- Recently added & Most watched
- Favorites
- Watch history
- Playlists

### 🔒 Private Vault
- Hide videos from main library
- PIN Lock
- Password Lock
- Auto-lock on app close
- SHA-256 encrypted credentials

### 💰 Monetization
- AdMob Banner Ads
- Interstitial Ads
- Rewarded Ads
- Auto-hide when Ad IDs empty

### 🎨 UI & Themes
- Material Design 3
- Light / Dark / System / Dynamic (Material You) themes
- Smooth animations
- Responsive layouts

### 🌐 Multi-Language
- English (en)
- বাংলা (bn)

### 📄 Legal Pages
- Privacy Policy (GDPR-friendly)
- Terms & Conditions
- About Us
- Contact Us
- Disclaimer

---

## 🏗️ Architecture

```
com.videviewer/
├── activities/          # 15 Activities
├── adapters/            # VideoAdapter, FolderAdapter
├── database/            # Room DB (6 entities, 5 DAOs)
├── fragments/           # 6 Fragments
├── models/              # VideoItem, FolderItem, Playlist
├── receivers/           # BootReceiver
├── services/            # PlaybackService
└── utils/               # AdManager, VaultManager, VideoScanner...
```

---

## 🚀 Build Instructions

### GitHub Actions (Automatic)
1. এই repo fork করুন
2. **Actions** ট্যাবে যান
3. **"Build Release APK"** workflow চালু করুন
4. Build শেষে **Artifacts** থেকে APK ডাউনলোড করুন

### Local Build
```bash
git clone https://github.com/sadiph858-spec/videviewer.git
cd videviewer
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚙️ Configuration

### AdMob Setup
`app/src/main/java/com/videviewer/utils/AppConstants.java` ফাইলে:
```java
public static final String TEST_BANNER_AD_ID = "ca-app-pub-XXXXXXX/XXXXXXX";
public static final String TEST_INTERSTITIAL_AD_ID = "ca-app-pub-XXXXXXX/XXXXXXX";
public static final String TEST_REWARDED_AD_ID = "ca-app-pub-XXXXXXX/XXXXXXX";
```

### google-services.json
Firebase/AdMob থেকে নিজের `google-services.json` দিয়ে `app/google-services.json` replace করুন।

---

## 📋 Requirements
- Android Studio Hedgehog (2023.1.1) বা তার পরের version
- JDK 17
- Android SDK 34
- Gradle 8.2

---

## 🛡️ Privacy
- কোনো ডেটা server এ পাঠানো হয় না
- সব data locally stored
- Vault videos encrypted app-private storage এ থাকে
- GDPR compliant

---

## 📞 Contact
- Email: support@videviewer.com
- GitHub Issues: [Report a bug](../../issues)

---

## 📜 License
```
Copyright 2025 VidéViewer Team

Licensed under the Apache License, Version 2.0
```

---

<p align="center">Made with ❤️ by VidéViewer Team</p>
