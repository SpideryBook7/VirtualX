package dev.vpad.controller.input

import android.util.Log
import java.io.File

/**
 * Checks at runtime whether /dev/uinput is accessible to this process.
 * If it is, a future update can use uinput to register a real Xbox One
 * virtual device (VID 0x045E / PID 0x02EA) visible to all apps.
 */
object UinputChecker {
    private const val TAG = "UinputChecker"

    data class UinputStatus(val available: Boolean, val details: String)

    fun check(): UinputStatus {
        return try {
            val f = File("/dev/uinput")
            // Try opening for write — uinput requires write access
            f.outputStream().close()
            Log.i(TAG, "uinput: ACCESSIBLE — true virtual device creation possible!")
            UinputStatus(true, "writable")
        } catch (e: SecurityException) {
            Log.i(TAG, "uinput: blocked by SELinux/permission: ${e.message}")
            UinputStatus(false, "SELinux blocked")
        } catch (e: Exception) {
            Log.i(TAG, "uinput: not accessible — ${e.message}")
            UinputStatus(false, e.message ?: "inaccessible")
        }
    }
}
