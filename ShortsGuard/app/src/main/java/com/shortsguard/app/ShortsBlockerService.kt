package com.shortsguard.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class ShortsBlockerService : AccessibilityService() {

    private var lastActionTime: Long = 0
    private val actionCooldownMs: Long = 800 // Prevent rapid loops
    private val mainHandler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var overlayMaskView: View? = null
    private var isOverlayAttached: Boolean = false
    private var lastOverlayBounds: Rect = Rect()

    companion object {
        var isServiceRunning: Boolean = false
            private set

        private const val PKG_YOUTUBE = "com.google.android.youtube"
        private val CHROME_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.canary",
            "com.chrome.dev"
        )
        private val WHATSAPP_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        removeOverlayMask()
    }

    override fun onInterrupt() {
        isServiceRunning = false
        removeOverlayMask()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 1. Hide overlay when user is not inside YouTube
        if (packageName != PKG_YOUTUBE) {
            removeOverlayMask()
        }

        // 2. Pre-emptive Click Interception: Catch the tap on "Shorts" BEFORE it opens
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val text = event.text.joinToString(" ")
            val desc = event.contentDescription?.toString() ?: ""
            if (text.contains("Shorts", ignoreCase = true) || desc.contains("Shorts", ignoreCase = true)) {
                triggerBlockAction("Shorts Button Click")
                return
            }
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastActionTime < actionCooldownMs) {
            return
        }

        when {
            packageName == PKG_YOUTUBE && BlockPreferences.isYoutubeBlockEnabled(this) -> {
                handleYoutubeEvent(event)
            }
            CHROME_PACKAGES.contains(packageName) && BlockPreferences.isChromeBlockEnabled(this) -> {
                handleChromeEvent(event)
            }
            WHATSAPP_PACKAGES.contains(packageName) && BlockPreferences.isWhatsappBlockEnabled(this) -> {
                handleWhatsappEvent(event)
            }
        }
    }

    /**
     * Blocks YouTube Shorts and updates the visual overlay mask
     */
    private fun handleYoutubeEvent(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""

        // 1. If user is already on a Shorts/Reel activity -> Exit immediately
        if (className.contains("ReelWatchActivity", ignoreCase = true) ||
            className.contains("ShortsActivity", ignoreCase = true)
        ) {
            triggerBlockAction("YouTube Shorts")
            return
        }

        val rootNode = rootInActiveWindow ?: return
        try {
            // 2. Visually mask and hide the Shorts button if enabled
            if (BlockPreferences.isHideShortsButtonEnabled(this) && Settings.canDrawOverlays(this)) {
                val shortsNode = findShortsButtonNode(rootNode)
                if (shortsNode != null) {
                    val rect = Rect()
                    shortsNode.getBoundsInScreen(rect)
                    if (rect.width() > 0 && rect.height() > 0) {
                        applyOverlayMask(rect)
                    }
                    shortsNode.recycle()
                }
            }

            // 3. Check if Shorts player view is actively showing
            if (isShortsPlayerActive(rootNode)) {
                triggerBlockAction("YouTube Shorts")
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Finds the Shorts button node in the bottom navigation bar
     */
    private fun findShortsButtonNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""

        if (desc.equals("Shorts", ignoreCase = true) || text.equals("Shorts", ignoreCase = true)) {
            return AccessibilityNodeInfo.obtain(node)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = findShortsButtonNode(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    /**
     * Positions a physical overlay over the Shorts button so it is visually covered and unclickable
     */
    private fun applyOverlayMask(bounds: Rect) {
        mainHandler.post {
            try {
                if (windowManager == null) return@post

                // Avoid unnecessary redraws if bounds haven't changed
                if (isOverlayAttached && lastOverlayBounds == bounds) {
                    return@post
                }

                lastOverlayBounds.set(bounds)

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    bounds.width(),
                    bounds.height(),
                    bounds.left,
                    bounds.top,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                if (overlayMaskView == null) {
                    overlayMaskView = View(this).apply {
                        // YouTube's dark bottom-bar background color #0F0F0F
                        setBackgroundColor(Color.parseColor("#0F0F0F"))
                        // Intercept touches on the masked area
                        setOnClickListener {
                            Toast.makeText(applicationContext, "🛡️ Shorts Button is Disabled!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                if (!isOverlayAttached) {
                    windowManager?.addView(overlayMaskView, params)
                    isOverlayAttached = true
                } else {
                    windowManager?.updateViewLayout(overlayMaskView, params)
                }
            } catch (e: Exception) {
                // Ignore window manager race conditions
            }
        }
    }

    private fun removeOverlayMask() {
        mainHandler.post {
            try {
                if (isOverlayAttached && overlayMaskView != null && windowManager != null) {
                    windowManager?.removeView(overlayMaskView)
                    isOverlayAttached = false
                    lastOverlayBounds.setEmpty()
                }
            } catch (e: Exception) {
                // Ignore if already removed
            }
        }
    }

    private fun isShortsPlayerActive(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName ?: ""

        if (viewId.contains("shorts_container", ignoreCase = true) ||
            viewId.contains("reel_player_page_tree", ignoreCase = true) ||
            viewId.contains("reel_recycler", ignoreCase = true) ||
            viewId.contains("reel_player", ignoreCase = true)
        ) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = isShortsPlayerActive(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    /**
     * Blocks Chrome Shorts / Reels URLs
     */
    private fun handleChromeEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        try {
            val urlBarNodes = rootNode.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
            if (urlBarNodes.isNotEmpty()) {
                val urlBar = urlBarNodes[0]
                val urlText = urlBar.text?.toString()?.lowercase() ?: ""
                urlBar.recycle()

                if (urlText.contains("youtube.com/shorts") ||
                    urlText.contains("/shorts/") ||
                    urlText.contains("instagram.com/reels") ||
                    urlText.contains("tiktok.com")
                ) {
                    triggerBlockAction("Browser Shorts")
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Blocks WhatsApp Status / Updates
     */
    private fun handleWhatsappEvent(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""

        if (className.contains("StatusPlaybackActivity", ignoreCase = true) ||
            className.contains("StatusPlaybackFragment", ignoreCase = true)
        ) {
            triggerBlockAction("WhatsApp Status")
            return
        }

        val rootNode = rootInActiveWindow ?: return
        try {
            if (isWhatsappUpdatesTabSelected(rootNode)) {
                triggerBlockAction("WhatsApp Updates Feed")
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun isWhatsappUpdatesTabSelected(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        val isUpdatesTab = text.equals("Updates", ignoreCase = true) ||
                text.equals("Status", ignoreCase = true) ||
                desc.contains("Updates tab", ignoreCase = true) ||
                desc.contains("Status tab", ignoreCase = true)

        if (isUpdatesTab && (node.isSelected || node.isFocused)) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = isWhatsappUpdatesTabSelected(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    /**
     * Performs instant back action, logs stats, and alerts user
     */
    private fun triggerBlockAction(source: String) {
        lastActionTime = System.currentTimeMillis()

        performGlobalAction(GLOBAL_ACTION_BACK)

        val todayCount = BlockPreferences.incrementBlockedCount(this)
        val isLockdown = BlockPreferences.isLockdownActive(this)

        mainHandler.post {
            val message = if (isLockdown) {
                val remaining = BlockPreferences.getRemainingLockdownMillis(this)
                val timeStr = BlockPreferences.formatRemainingTime(remaining)
                "🔒 STRICT LOCKDOWN ACTIVE! ($timeStr remaining)\n$source blocked!"
            } else {
                "🛡️ $source blocked! (Blocked $todayCount today)"
            }

            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
