package com.toddfacil.omar

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Cuando Todd TV pasa a primer plano, espera unos segundos, vuelve al menú
 * (tecla Atrás) si hace falta, y toca la opción "TV en vivo".
 */
class TvEnVivoService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val cache = HashMap<String, Boolean>()
    private var enTodd = false
    private var hecho = false
    private var intentos = 0
    private var apretoAtras = false

    private val paso = Runnable { hacerPaso() }

    override fun onServiceConnected() {
        Toast.makeText(this, "Todd Fácil activado", Toast.LENGTH_LONG).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return

        if (esTodd(pkg)) {
            if (!enTodd) {
                enTodd = true
                hecho = false
                intentos = 0
                apretoAtras = false
                handler.removeCallbacks(paso)
                handler.postDelayed(paso, 4000)
            }
        } else {
            enTodd = false
            handler.removeCallbacks(paso)
        }
    }

    private fun hacerPaso() {
        if (!enTodd || hecho) return
        intentos++
        val root = rootInActiveWindow
        if (root != null && esTodd(root.packageName?.toString() ?: "")) {
            val objetivo = root.findAccessibilityNodeInfosByText("TV en vivo")?.firstOrNull()
            if (objetivo != null) {
                if (tocar(objetivo)) {
                    hecho = true
                    return
                }
            } else if (!apretoAtras) {
                apretoAtras = true
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
        if (intentos < 20) handler.postDelayed(paso, 1500)
    }

    private fun tocar(nodo: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = nodo
        while (n != null) {
            if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            n = n.parent
        }
        if (Build.VERSION.SDK_INT >= 33) {
            nodo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            return performGlobalAction(GLOBAL_ACTION_DPAD_CENTER)
        }
        return false
    }

    private fun esTodd(pkg: String): Boolean {
        if (pkg.isEmpty()) return false
        return cache.getOrPut(pkg) {
            if (pkg.contains("todd", ignoreCase = true)) true
            else try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(info).toString().contains("todd", ignoreCase = true)
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onInterrupt() {}
}
