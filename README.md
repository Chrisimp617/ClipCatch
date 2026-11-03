# ClipCatch

ClipCatch is an Android app starter project using the yt-dlp Android wrapper.

## Features
- Download as MP4 or extract audio as MP3
- Select maximum video height: best available, 1080p, 720p, 480p or 360p
- Download progress display
- Output folder: `Downloads/ClipCatch`

## Build an APK with GitHub Actions
1. Open the **Actions** tab in this repository.
2. Select **Build ClipCatch APK**.
3. If prompted, enable GitHub Actions for the repository.
4. Choose **Run workflow** and run it on `main`.
5. Open the completed workflow run and download the `ClipCatch-debug-apk` artifact.
6. Extract the downloaded ZIP; install `app-debug.apk` on your Android phone.

A push to `main` also starts a build automatically.

## Build configuration
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Java 17
- Compile / target SDK 35
- Minimum SDK 24
- yt-dlp Android wrapper 0.18.1

## Important
This is an initial project. The first APK build must be checked in GitHub Actions, and the app has not yet been device-tested. Download only material you own or are authorised to save, and comply with relevant platform terms and copyright law.
