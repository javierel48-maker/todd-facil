package com.toddfacil.omar

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Registro {
    private const val PREFS = "registro"

    fun anotar(ctx: Context, msg: String) {
        try {
            val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val lineas = (sp.getString("l", "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
            lineas.add(0, hora + " " + msg.replace("\n", " "))
            sp.edit().putString("l", lineas.take(20).joinToString("\n")).apply()
        } catch (e: Throwable) {
        }
    }

    fun leer(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("l", "") ?: ""

    fun borrar(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

class TvEnVivoService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val cache = HashMap<String, Boolean>()
    private var enTodd = false
    private var ultimoPaquete = ""
    private var ultimoInicio = 0L
    private var paso = 0
    private var esperas = 0

    private val receptor = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            try {
                when (i?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        anotar("Pantalla APAGADA")
                        enTodd = false
                        ultimoInicio = 0L
                        handler.removeCallbacksAndMessages(null)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        anotar("Pantalla ENCENDIDA")
                        iniciar(5000)
                    }
                }
            } catch (e: Throwable) {
                anotar("Error en receptor")
            }
        }
    }

    private fun anotar(msg: String) = Registro.anotar(this, msg)

    override fun onServiceConnected() {
        try {
            val f = IntentFilter()
            f.addAction(Intent.ACTION_SCREEN_ON)
            f.addAction(Intent.ACTION_SCREEN_OFF)
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(receptor, f, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(receptor, f)
            }
            anotar("Servicio conectado")
        } catch (e: Throwable) {
            anotar("No pude registrar receptor")
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
            if (pkg == packageName) return
            if (pkg != ultimoPaquete) {
                ultimoPaquete = pkg
                anotar("Ventana: " + pkg)
            }
            if (pkg == "com.android.systemui") return
            if (esTodd(pkg)) {
                if (!enTodd) {
                    enTodd = true
                    anotar("Todd TV al frente")
                    iniciar(4000)
                }
            } else {
                enTodd = false
                ultimoInicio = 0L
                handler.removeCallbacksAndMessages(null)
            }
        } catch (e: Throwable) {
        }
    }

    private fun iniciar(demora: Long) {
        val ahora = System.currentTimeMillis()
        if (ahora - ultimoInicio < 20000) return
        ultimoInicio = ahora
        handler.removeCallbacksAndMessages(null)
        paso = 0
        esperas = 0
        anotar("Inicio en " + (demora / 1000) + " s")
        handler.postDelayed({ ejecutar() }, demora)
    }

    private fun ejecutar() {
        try {
            if (!toddAdelante()) {
                esperas++
                if (esperas > 20) {
                    anotar("Todd TV no está al frente. Cancelo")
                    return
                }
                handler.postDelayed({ ejecutar() }, 1000)
                return
            }
            if (Build.VERSION.SDK_INT < 33) {
                anotar("Android menor a 13: no puedo apretar teclas")
                return
            }
            when (paso) {
                0 -> {
                    anotar("Veo: " + veo())
                    anotar("Aprieto Atrás")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    paso = 1
                    handler.postDelayed({ ejecutar() }, 1800)
                }
                1 -> {
                    anotar("Veo: " + veo())
                    val ok = performGlobalAction(GLOBAL_ACTION_DPAD_DOWN)
                    anotar("Aprieto Abajo: " + ok)
                    paso = 2
                    handler.postDelayed({ ejecutar() }, 900)
                }
                2 -> {
                    val ok = performGlobalAction(GLOBAL_ACTION_DPAD_CENTER)
                    anotar("Aprieto OK: " + ok)
                    paso = 3
                    handler.postDelayed({ anotar("Al final veo: " + veo()) }, 3000)
                }
            }
        } catch (e: Throwable) {
            anotar("Error al ejecutar")
        }
    }

    private fun toddAdelante(): Boolean {
        val root = rootInActiveWindow ?: return false
        return esTodd(root.packageName?.toString() ?: "")
    }

    private fun veo(): String {
        val l = mutableListOf<String>()
        textos(rootInActiveWindow, l)
        return if (l.isEmpty()) "(sin textos)" else l.joinToString(" | ")
    }

    private fun textos(nodo: AccessibilityNodeInfo?, salida: MutableList<String>) {
        if (nodo == null || salida.size >= 8) return
        val t = nodo.text?.toString()
        if (!t.isNullOrBlank()) salida.add(t.take(25).replace("\n", " "))
        for (i in 0 until nodo.childCount) {
            textos(nodo.getChild(i), salida)
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
