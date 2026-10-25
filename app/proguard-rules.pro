# R8 full mode: só mantém o essencial.
-keep class dev.nova.launcher.admin.LockDeviceAdminReceiver { *; }
-keep class dev.nova.launcher.access.LockAccessibilityService { *; }
-keepattributes *Annotation*
