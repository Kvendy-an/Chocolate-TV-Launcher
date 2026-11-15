package com.kvendy.chocolatetvlauncher.remote

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.kvendy.chocolatetvlauncher.ACTION_OPEN_PREFERENCES
import com.kvendy.chocolatetvlauncher.MainActivity
import com.kvendy.chocolatetvlauncher.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The launcher activity, learning dialog, and input callback all run on the main thread. */
internal object RemoteButtons {
    private val connectedState = MutableStateFlow(false)
    val connected = connectedState.asStateFlow()

    var launcherForeground = false
    var learnButton: ((Int) -> Unit)? = null

    fun setConnected(connected: Boolean) {
        connectedState.value = connected
    }
}

class RemoteButtonService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var mappings = emptyMap<Int, RemoteMapping>()
    private var overrideHome = false
    private val pendingPresses = mutableMapOf<Press, PendingAction>()

    private data class Press(val deviceId: Int, val keyCode: Int)
    private sealed interface PendingAction {
        data class Learn(val callback: (Int) -> Unit) : PendingAction
        data class Run(val mapping: RemoteMapping) : PendingAction
        data object OpenChocolateHome : PendingAction
    }

    override fun onCreate() {
        super.onCreate()
        val store = RemoteMappings(this)
        scope.launch { store.mappings.collect { mappings = it } }
        scope.launch { store.overrideHome.collect { overrideHome = it } }
    }

    override fun onServiceConnected() {
        RemoteButtons.setConnected(true)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        RemoteButtons.setConnected(false)
        pendingPresses.clear()
        handler.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        RemoteButtons.setConnected(false)
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!overrideHome && mappings.isEmpty() && pendingPresses.isEmpty() && RemoteButtons.learnButton == null) return false
        val keyCode = event.keyCode
        val isHome = keyCode == KeyEvent.KEYCODE_HOME
        if (keyCode in protectedKeys) return false
        val press = Press(event.deviceId, keyCode)
        if (event.action == KeyEvent.ACTION_UP) {
            val pending = pendingPresses.remove(press) ?: return false
            if (!event.isCanceled) handler.post { completePress(keyCode, pending) }
            return true
        }
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount > 0) return press in pendingPresses
        pendingPresses.remove(press)

        val pending = if (isHome) {
            if (!overrideHome) return false
            PendingAction.OpenChocolateHome
        } else {
            val learn = RemoteButtons.learnButton?.takeIf { RemoteButtons.launcherForeground }
            val mapping = mappings[keyCode]?.takeIf { it.isActive() }
            when {
                learn != null -> PendingAction.Learn(learn)
                mapping != null -> PendingAction.Run(mapping)
                else -> return false
            }
        }

        val device = event.device ?: return false
        if (device.isVirtual || device.name in virtualRemoteNames) return false

        pendingPresses[press] = pending
        return true
    }

    private fun RemoteMapping.isActive() = scope == RemoteScope.Everywhere || RemoteButtons.launcherForeground

    private fun completePress(keyCode: Int, pending: PendingAction) {
        when (pending) {
            is PendingAction.Learn -> if (RemoteButtons.learnButton === pending.callback && RemoteButtons.launcherForeground) {
                RemoteButtons.learnButton = null
                pending.callback(keyCode)
            }
            is PendingAction.Run -> if (mappings[keyCode] == pending.mapping && pending.mapping.isActive()) {
                runAction(pending.mapping.action, pending.mapping.packageName)
            }
            PendingAction.OpenChocolateHome -> if (overrideHome) runAction(RemoteAction.OpenLauncher)
        }
    }

    private fun runAction(action: RemoteAction, packageName: String? = null) {
        try {
            when (action) {
                RemoteAction.LaunchApp -> {
                    val target = checkNotNull(packageName)
                    val intent = packageManager.getLeanbackLaunchIntentForPackage(target)
                        ?: packageManager.getLaunchIntentForPackage(target)
                        ?: throw ActivityNotFoundException()
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                RemoteAction.OpenLauncher, RemoteAction.OpenPreferences -> {
                    val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (action == RemoteAction.OpenPreferences) intent.action = ACTION_OPEN_PREFERENCES
                    startActivity(intent)
                }
                RemoteAction.Screenshot -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        saveRemoteScreenshot()
                    } else if (!action.isSupported || !performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)) {
                        Toast.makeText(this, R.string.remote_action_unavailable, Toast.LENGTH_SHORT).show()
                    }
                }
                RemoteAction.Settings -> startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                RemoteAction.Back -> performGlobalAction(GLOBAL_ACTION_BACK)
                RemoteAction.ToggleMute -> getSystemService(AudioManager::class.java)
                    .adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                RemoteAction.Ignore -> Unit
            }
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.remote_action_unavailable, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.remote_action_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private companion object {
        val virtualRemoteNames = setOf("virtual-remote", "virtual-search")
        val protectedKeys = setOf(
            KeyEvent.KEYCODE_UNKNOWN, KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_TV_POWER, KeyEvent.KEYCODE_SLEEP,
            KeyEvent.KEYCODE_WAKEUP
        )
    }
}
