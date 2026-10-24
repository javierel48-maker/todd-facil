package com.toddfacil.omar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onResume() {
        super.onResume()
        val activo = servicioActivo()

        val texto = TextView(this).apply {
            textSize = 30f
            gravity = Gravity.CENTER
            text = if (activo)
                "Todd Fácil para Omar está ACTIVADO.\n\nYa puede cerrar esta pantalla."
            else
                "Falta un paso:\nactivar «Todd Fácil para Omar» en Accesibilidad."
        }
        val boton = Button(this).apply {
            textSize = 26f
            text = "Abrir Accesibilidad"
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
            gravity = Gravity.CENTER
            setPadding(80, 80, 80, 80)
            addView(texto)
            addView(boton)
        }
        setContentView(layout)
        boton.requestFocus()
    }

    private fun servicioActivo(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains(packageName)
    }
}
