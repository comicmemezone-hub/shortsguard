package com.shortsguard.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.shortsguard.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateStats()
        loadSwitchStates()
    }

    private fun setupListeners() {
        binding.btnEnableService.setOnClickListener {
            // Open Android Accessibility Settings directly
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.switchHideShortsButton.setOnCheckedChangeListener { _, isChecked ->
            BlockPreferences.setHideShortsButtonEnabled(this, isChecked)
            if (isChecked && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        binding.switchYoutube.setOnCheckedChangeListener { _, isChecked ->
            BlockPreferences.setYoutubeBlockEnabled(this, isChecked)
        }

        binding.switchChrome.setOnCheckedChangeListener { _, isChecked ->
            BlockPreferences.setChromeBlockEnabled(this, isChecked)
        }

        binding.switchWhatsapp.setOnCheckedChangeListener { _, isChecked ->
            BlockPreferences.setWhatsappBlockEnabled(this, isChecked)
        }
    }

    private fun loadSwitchStates() {
        binding.switchHideShortsButton.isChecked = BlockPreferences.isHideShortsButtonEnabled(this)
        binding.switchYoutube.isChecked = BlockPreferences.isYoutubeBlockEnabled(this)
        binding.switchChrome.isChecked = BlockPreferences.isChromeBlockEnabled(this)
        binding.switchWhatsapp.isChecked = BlockPreferences.isWhatsappBlockEnabled(this)
    }

    private fun updateStats() {
        val count = BlockPreferences.getTodayBlockedCount(this)
        binding.tvBlockedCount.text = count.toString()
    }

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled(this, ShortsBlockerService::class.java)

        if (isEnabled || ShortsBlockerService.isServiceRunning) {
            binding.statusIndicator.setBackgroundColor(
                ContextCompat.getColor(this, R.color.accent_green)
            )
            binding.tvStatusText.text = getString(R.string.service_status_active)
            binding.tvStatusDescription.text = "ShortsGuard is actively monitoring and protecting your focus."
            binding.btnEnableService.text = "Service Settings"
        } else {
            binding.statusIndicator.setBackgroundColor(
                ContextCompat.getColor(this, R.color.accent_red)
            )
            binding.tvStatusText.text = getString(R.string.service_status_inactive)
            binding.tvStatusDescription.text = "Tap below to turn ON ShortsGuard in your phone's Accessibility settings."
            binding.btnEnableService.text = getString(R.string.enable_service_btn)
        }
    }

    private fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<*>
    ): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false

        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        for (enabledService in enabledServices) {
            val serviceInfo = enabledService.resolveInfo.serviceInfo
            if (serviceInfo.packageName == context.packageName &&
                serviceInfo.name == serviceClass.name
            ) {
                return true
            }
        }
        return false
    }
}
