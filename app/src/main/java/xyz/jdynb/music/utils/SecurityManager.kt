package xyz.jdynb.music.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Debug
import android.os.Process
import android.text.TextUtils
import xyz.jdynb.music.BuildConfig
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.system.exitProcess

object SecurityManager {

    private const val TAG = "SecurityManager"

    fun check(context: Context) {
        if (BuildConfig.DEBUG) {
            return // 允许在 debug 构建中调试，或者删除此行以进行严格测试
        }

        /*if (isDeviceRooted()) {
            killApp("Root detected")
        }*/
        if (isDebuggerAttached(context)) {
            // killApp("Debugger detected")
        }
        if (isEmulator()) {
            killApp("Emulator detected")
        }
        if (isProxyEnabled(context)) {
            killApp("Proxy detected")
        }
        if (isHookDetected()) {
            killApp("Hook detected")
        }
    }

    private fun killApp(reason: String) {
        // 如果需要，记录原因，但为了安全起见，静默退出通常更好，或者直接崩溃
        android.util.Log.e(TAG, "Security violation: $reason")
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    // --- Root 检测 ---
    private fun isDeviceRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        val paths =
                arrayOf(
                        "/system/app/Superuser.apk",
                        "/sbin/su",
                        "/system/bin/su",
                        "/system/xbin/su",
                        "/data/local/xbin/su",
                        "/data/local/bin/su",
                        "/system/sd/xbin/su",
                        "/system/bin/failsafe/su",
                        "/data/local/su",
                        "/su/bin/su"
                )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    // --- 调试检测 ---
    private fun isDebuggerAttached(context: Context): Boolean {
        if (Debug.isDebuggerConnected()) return true

        val info = context.applicationInfo
        if ((info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) return true

        return false
    }

    // --- 模拟器检测 ---
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT)
    }

    // --- 代理/VPN 检测 ---
    private fun isProxyEnabled(context: Context): Boolean {
        val proxyHost = System.getProperty("http.proxyHost")
        val proxyPort = System.getProperty("http.proxyPort")
        if (!TextUtils.isEmpty(proxyHost) && !TextUtils.isEmpty(proxyPort)) {
            return true
        }

        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)

        if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            // 严格模式：禁止 VPN
            return true
        }

        return false
    }

    // --- Hook 检测 (Xposed/Frida) ---
    private fun isHookDetected(): Boolean {
        try {
            throw Exception("Hook Check")
        } catch (e: Exception) {
            for (stackTraceElement in e.stackTrace) {
                if (stackTraceElement.className.contains("de.robv.android.xposed.XposedBridge") ||
                                stackTraceElement.className.contains("com.saurik.substrate.MS$2")
                ) {
                    return true
                }
            }
        }

        // 可以在此处添加针对 Frida 特定文件/端口的检查，
        // 但堆栈跟踪和 maps 检查通常对基本脚本足够有效。
        if (checkFridaMaps()) return true

        return false
    }

    private fun checkFridaMaps(): Boolean {
        try {
            val file = File("/proc/self/maps")
            if (file.exists()) {
                val reader = BufferedReader(FileReader(file))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("frida")) {
                        return true
                    }
                }
                reader.close()
            }
        } catch (e: Exception) {
            // 忽略
        }
        return false
    }
}
