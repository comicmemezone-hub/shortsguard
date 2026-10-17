# 🛡️ ShortsGuard - Android Short-Form Content Blocker

**ShortsGuard** is a 100% free, private, on-device Android app designed to completely block doomscrolling feeds across your phone:
* **🔒 Unbreakable Deadline Lockdown Mode:** Set a deadline (30 min, 1h, 2h, 4h, 8h, or custom time). Once activated, all blocking settings are locked ON and CANNOT be disabled or toggled off until the countdown deadline expires.
* **Visually Hide Shorts Button:** Places a sleek, protective mask over the bottom Shorts button in the YouTube app so the button is completely covered, hidden, and unclickable!
* **Pre-emptive Click Interception:** Tapping the Shorts area is caught instantly before YouTube can even buffer or open the player.
* **YouTube App:** Auto-exits any opened Shorts feed or Reel player.
* **Chrome / Mobile Browsers:** Detects `youtube.com/shorts`, `instagram.com/reels`, and `tiktok.com` in the URL bar and backs out automatically.
* **WhatsApp:** Blocks the distracting "Updates" / "Status" feed and playback.

---

## 🔒 Privacy & Battery
* **Zero Internet Permissions:** Cannot send data anywhere.
* **100% Local:** Runs purely on your phone via the Android Accessibility Service.
* **Lightweight:** Does not run background battery-draining polling tasks; only reacts when you actively navigate to shorts.

---

## 🚀 How to Build into an APK and Install on Your Phone

You have two simple options to get the APK onto your phone:

### Option A: Free Cloud Build via GitHub (No Android Studio required!)
If you do not want to download 10GB of Android Studio:
1. Upload this `ShortsGuard` folder to a repository on [GitHub](https://github.com).
2. Go to the **Actions** tab on your GitHub repository.
3. The build workflow will automatically run and compile the APK!
4. Download `ShortsGuard-Debug-APK` directly to your phone.
5. Tap the `.apk` file to install it.

### Option B: Local Build via Android Studio
1. Open **Android Studio** on your PC.
2. Click **File -> Open...** and select the `ShortsGuard` folder.
3. Connect your Android phone with a USB cable (enable USB Debugging).
4. Click the green **Run (Play)** button, or go to **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
5. Transfer and install `app-debug.apk` to your phone.

---

## 📱 How to Activate on Your Phone

Once installed:
1. Open **ShortsGuard** on your phone.
2. Tap **"Enable in Accessibility Settings"**.
3. In your phone's Settings:
   * Go to **Accessibility** -> **Downloaded Apps** (or Installed Services).
   * Tap **ShortsGuard Blocker**.
   * Toggle it **ON** and accept the permission.
4. Return to the app — the status card will turn **Green (Shield Active)**!
5. Test it: Open YouTube and tap a Short, or open WhatsApp Updates tab — ShortsGuard will instantly bounce you back and protect your focus!
