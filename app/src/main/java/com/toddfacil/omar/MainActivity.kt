package com.toddfacil.omar

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    override fun onResume() {
        super.onResume()
        armar()
    }

    private fun armar() {
        val activo = servicioActivo()

        val estado = TextView(this).apply {
            textSize = 26f
            gravity = Gravity.CENTER
            text = if (activo) "Todd Fácil para Omar: ACTIVADO"
            else "Falta activar «Todd Fácil para Omar» en Accesibilidad"
        }

        val r = Registro.leer(this)
        val registro = TextView(this).apply {
            textSize = 16f
            setPadding(0, 30, 0, 30)
            text = if (r.isEmpty()) "(registro vacío)" else r
        }

        val botonActualizar = Button(this).apply {
            text = "Actualizar registro"
            textSize = 20f
            setOnClickListener { armar() }
        }
        val botonBorrar = Button(this).apply {
            text = "Borrar registro"
            textSize = 20f
            setOnClickListener {
                Registro.borrar(this@MainActivity)
                armar()
            }
        }
        val botonAcc = Button(this).apply {
            text = "Abrir Accesibilidad"
            textSize = 20f
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
            addView(estado)
            addView(registro)
            addView(botonActualizar)
            addView(botonBorrar)
            addView(botonAcc)
        }

        setContentView(ScrollView(this).apply { addView(layout) })
        botonActualizar.requestFocus()
    }

    private fun servicioActivo(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains(packageName)
    }
}
