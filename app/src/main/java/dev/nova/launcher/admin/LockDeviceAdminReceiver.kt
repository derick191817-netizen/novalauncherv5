package dev.nova.launcher.admin

import android.app.admin.DeviceAdminReceiver

/** Receiver de Device Admin, usado somente para `lockNow()` (política `force-lock`; não toca em políticas do Knox). */
class LockDeviceAdminReceiver : DeviceAdminReceiver()
