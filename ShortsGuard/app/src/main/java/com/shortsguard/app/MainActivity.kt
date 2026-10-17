package com.shortsguard.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shortsguard.app.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var countdownTimer: CountDownTimer? = null

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
        checkLockdownState()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }

    private fun setupListeners() {
        binding.btnEnableService.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.switchHideShortsButton.setOnCheckedChangeListener { _, isChecked ->
            if (BlockPreferences.isLockdownActive(this)) {
                binding.switchHideShortsButton.isChecked = true
                showLockdownToast()
                return@setOnCheckedChangeListener
            }
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
            if (BlockPreferences.isLockdownActive(this)) {
                binding.switchYoutube.isChecked = true
                showLockdownToast()
                return@setOnCheckedChangeListener
            }
            BlockPreferences.setYoutubeBlockEnabled(this, isChecked)
        }

        binding.switchChrome.setOnCheckedChangeListener { _, isChecked ->
            if (BlockPreferences.isLockdownActive(this)) {
                binding.switchChrome.isChecked = true
                showLockdownToast()
                return@setOnCheckedChangeListener
            }
            BlockPreferences.setChromeBlockEnabled(this, isChecked)
        }

        binding.switchWhatsapp.setOnCheckedChangeListener { _, isChecked ->
            if (BlockPreferences.isLockdownActive(this)) {
                binding.switchWhatsapp.isChecked = true
                showLockdownToast()
                return@setOnCheckedChangeListener
            }
            BlockPreferences.setWhatsappBlockEnabled(this, isChecked)
        }

        binding.btnStartLockdown.setOnClickListener {
            handleStartLockdownClick()
        }

        binding.chipCustom.setOnClickListener {
            showCustomTimePicker()
        }
    }

    private fun handleStartLockdownClick() {
        val durationMillis: Long = when {
            binding.chip30m.isChecked -> 30 * 60 * 1000L
            binding.chip1h.isChecked -> 60 * 60 * 1000L
            binding.chip2h.isChecked -> 2 * 60 * 60 * 1000L
            binding.chip4h.isChecked -> 4 * 60 * 60 * 1000L
            binding.chip8h.isChecked -> 8 * 60 * 60 * 1000L
            binding.chipCustom.isChecked -> {
                showCustomTimePicker()
                return
            }
            else -> 60 * 60 * 1000L
        }

        val targetDeadline = System.currentTimeMillis() + durationMillis
        confirmAndStartLockdown(targetDeadline)
    }

    private fun showCustomTimePicker() {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        val picker = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                }

                // If picked time is earlier today, assume it's for tomorrow
                if (targetCal.timeInMillis <= System.currentTimeMillis()) {
                    targetCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                confirmAndStartLockdown(targetCal.timeInMillis)
            },
            currentHour,
            currentMinute,
            false
        )
        picker.show()
    }

    private fun confirmAndStartLockdown(targetDeadlineMs: Long) {
        val formattedDeadline = BlockPreferences.formatDeadlineDateTime(targetDeadlineMs)

        MaterialAlertDialogBuilder(this)
            .setTitle("🔒 Start Unbreakable Lockdown?")
            .setMessage(
                "Once activated, ALL Shorts, Reels, and Statuses are STRICTLY BLOCKED until:\n\n" +
                "👉 $formattedDeadline\n\n" +
                "⚠️ This lockdown is UNBREAKABLE. You will not be able to turn off blocking or watch any short-form content until the deadline completes."
            )
            .setPositiveButton("Lock Down Now") { _, _ ->
                BlockPreferences.setCustomDeadline(this, targetDeadlineMs)
                Toast.makeText(this, "🔒 Strict Focus Lockdown Started!", Toast.LENGTH_LONG).show()
                checkLockdownState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkLockdownState() {
        countdownTimer?.cancel()

        if (BlockPreferences.isLockdownActive(this)) {
            // Lockdown is actively running
            binding.layoutLockdownInactive.visibility = View.GONE
            binding.layoutLockdownActive.visibility = View.VISIBLE

            val deadlineMs = BlockPreferences.getLockdownDeadlineMs(this)
            binding.tvLockdownDeadlineInfo.text = "Deadline: ${BlockPreferences.formatDeadlineDateTime(deadlineMs)}"

            // Lock all switches ON and prevent user from disabling them
            binding.switchHideShortsButton.apply {
                isChecked = true
                isEnabled = false
            }
            binding.switchYoutube.apply {
                isChecked = true
                isEnabled = false
            }
            binding.switchChrome.apply {
                isChecked = true
                isEnabled = false
            }
            binding.switchWhatsapp.apply {
                isChecked = true
                isEnabled = false
            }

            val remaining = BlockPreferences.getRemainingLockdownMillis(this)
            binding.tvLockdownCountdown.text = BlockPreferences.formatRemainingTime(remaining)

            countdownTimer = object : CountDownTimer(remaining, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    binding.tvLockdownCountdown.text = BlockPreferences.formatRemainingTime(millisUntilFinished)
                }

                override fun onFinish() {
                    Toast.makeText(
                        applicationContext,
                        "🎉 Congratulations! Focus deadline reached.",
                        Toast.LENGTH_LONG
                    ).show()
                    checkLockdownState()
                }
            }.start()

        } else {
            // No active lockdown
            binding.layoutLockdownInactive.visibility = View.VISIBLE
            binding.layoutLockdownActive.visibility = View.GONE

            // Re-enable toggles
            binding.switchHideShortsButton.isEnabled = true
            binding.switchYoutube.isEnabled = true
            binding.switchChrome.isEnabled = true
            binding.switchWhatsapp.isEnabled = true

            loadSwitchStates()
        }
    }

    private fun showLockdownToast() {
        Toast.makeText(this, "🔒 Cannot change settings during an active Lockdown!", Toast.LENGTH_SHORT).show()
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
