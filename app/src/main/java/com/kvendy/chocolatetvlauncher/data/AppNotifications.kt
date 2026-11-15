package com.kvendy.chocolatetvlauncher.data

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationManagerCompat

/** A single active notification from another app, as shown in the Home row / Notifications page. */
data class AppNotification(
    val key: String,
    val packageName: String,
    val postTime: Long,
    val title: String?,
    val text: String?,
    val contentIntent: PendingIntent?
)

/**
 * Reactive holder for currently-active notifications, same mutableStateListOf/mutableStateMapOf
 * singleton shape as ColorCache/CustomImageCache — [LauncherNotificationListenerService] (a real
 * Android component, managed by the system) writes into this on the main thread from its
 * callbacks; reading it inside a composable subscribes to those updates directly, no separate
 * event bus or binding needed since the service runs in this same process.
 */
object NotificationStore {
    // Defensive cap, same "trim at write time" approach RecentAppsTracker already uses — a
    // misbehaving app posting many notifications without ever clearing them shouldn't be able to
    // grow this list (and the per-post removeAll scan below) unbounded.
    private const val MAX_NOTIFICATIONS = 50

    val notifications = mutableStateListOf<AppNotification>()

    fun onPosted(sbn: StatusBarNotification) {
        val entry = AppNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            postTime = sbn.postTime,
            title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            contentIntent = sbn.notification.contentIntent
        )
        // Re-posted (updated) notifications reuse the same key — replace rather than duplicate.
        notifications.removeAll { it.key == entry.key }
        notifications.add(0, entry)
        while (notifications.size > MAX_NOTIFICATIONS) {
            notifications.removeAt(notifications.lastIndex)
        }
    }

    fun onRemoved(sbn: StatusBarNotification) {
        notifications.removeAll { it.key == sbn.key }
    }

    fun clear(context: Context) {
        notifications.toList().forEach { LauncherNotificationListenerService.dismiss(it.key) }
        notifications.clear()
    }
}

/**
 * Requires the user to manually grant "Notification access" in system settings — Android has no
 * runtime-permission dialog for this, only Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS (see
 * NotificationsPreference). Once granted, the system instantiates and binds this service itself.
 */
class LauncherNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        // Notifications posted before this listener connected (e.g. right after the user grants
        // access) wouldn't otherwise be picked up by onNotificationPosted, which only fires for
        // ones posted after connection.
        try {
            val active = activeNotifications.orEmpty()
            NotificationStore.notifications.clear()
            active.filter { it.packageName != packageName }.forEach { NotificationStore.onPosted(it) }
        } catch (e: SecurityException) {
            // Some TV firmwares deliver a stale connected callback after token revocation.
            // The next real connection will resynchronize the store.
            if (instance === this) instance = null
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        NotificationStore.onPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationStore.onRemoved(sbn)
    }

    companion object {
        private var instance: LauncherNotificationListenerService? = null

        /** No-op if the listener isn't currently connected (permission revoked, service killed). */
        fun dismiss(key: String) {
            try {
                instance?.cancelNotification(key)
            } catch (e: SecurityException) {
                instance = null
            }
        }

        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}
