package com.toddfacil.omar

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class TvEnVivoService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val cache = HashMap<String, Boolean>()
    private var enTodd = false
    private var hecho = false
    private var intentos = 0
    private var toques = 0
    private var apretoAtras = false
    private var vioTodd = false
    private val paso = Runnable { hacerPaso() }

    private val receptor = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            try {
                when (i?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        handler.removeCallbacks(paso)
                        enTodd = false
                    }
                    Intent.ACTION_SCREEN_ON -> iniciar(3000)
                }
            } catch (e: Throwable) {
            }
        }
    }

    override fun onServiceConnected() {
        try {
            val f = IntentFilter()
            f.addAction(Intent.ACTION_SCREEN_ON)
            f.addAction(Intent.ACTION_SCREEN_OFF)
            registerReceiver(receptor, f)
            aviso("Todd Fácil activado")
        } catch (e: Throwable) {
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receptor)
        } catch (e: Throwable) {
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
            val pkg = event.packageName?.toString() ?: return
            if (pkg == packageName || pkg == "com.android.systemui") return
            if (esTodd(pkg)) {
                if (!enTodd) {
                    enTodd = true
                    iniciar(4000)
                }
            } else {
                enTodd = false
                handler.removeCallbacks(paso)
            }
        } catch (e: Throwable) {
        }
    }

    private fun iniciar(demora: Long) {
        hecho = false
        intentos = 0
        toques = 0
        apretoAtras = false
        vioTodd = false
        handler.removeCallbacks(paso)
        handler.postDelayed(paso, demora)
    }

    private fun hacerPaso() {
        try {
            if (hecho) return
            intentos++
            val root = rootInActiveWindow
            if (root != null && esTodd(root.packageName?.toString() ?: "")) {
                vioTodd = true
                val objetivo = root.findAccessibilityNodeInfosByText("TV en vivo")?.firstOrNull()
                if (objetivo != null) {
                    if (toques == 0) {
                        aviso("Toco TV en vivo (clic)")
                        tocar(objetivo)
                        toques = 1
                    } else if (toques == 1) {
                        aviso("Toco TV en vivo (posición)")
                        tocarPosicion(objetivo)
                        toques = 2
                    } else {
                        aviso("TV en vivo no responde")
                        hecho = true
                        return
                    }
                } else if (toques > 0) {
                    aviso("Listo: entró a TV en vivo")
                    hecho = true
                    return
                } else if (!apretoAtras) {
                    apretoAtras = true
                    aviso("Apreto Atrás")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
            if (intentos < 20) {
                handler.postDelayed(paso, 1500)
            } else if (vioTodd) {
                aviso("No encontré TV en vivo. Veo: " + textos(root, mutableListOf()).joinToString(" | "), true)
            }
        } catch (e: Throwable) {
        }
    }

    private fun tocar(nodo: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = nodo
        while (n != null) {
            if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            n = n.parent
        }
        return false
    }

    private fun tocarPosicion(nodo: AccessibilityNodeInfo): Boolean {
        if (Build.VERSION.SDK_INT < 24) return false
        val r = Rect()
        nodo.getBoundsInScreen(r)
        if (r.isEmpty) return false
        val p = Path()
        p.moveTo(r.exactCenterX(), r.exactCenterY())
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 100))
            .build()
        return dispatchGesture(g, null, null)
    }

    private fun textos(nodo: AccessibilityNodeInfo?, salida: MutableList<String>): List<String> {
        if (nodo == null || salida.size >= 8) return salida
        val t = nodo.text?.toString()
        if (!t.isNullOrBlank()) salida.add(t)
        for (i in 0 until nodo.childCount) {
            textos(nodo.getChild(i), salida)
        }
        return salida
    }

    private fun aviso(msg: String, largo: Boolean = false) {
        try {
            Toast.makeText(this, msg, if (largo) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
        }
    }

    private fun esTodd(pkg: String): Boolean {
        if (pkg.isEmpty()) return false
        return cache.getOrPut(pkg) {
            if (pkg.contains("todd", ignoreCase = true)) true
            else try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(info).toString().contains("todd", ignoreCase = true)
            } catch (e: Throwable) {
                false
            }
        }
    }

    override fun onInterrupt() {}
}
