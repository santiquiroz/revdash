package com.revdash.espejo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.net.Inet4Address

class MainActivity : Activity() {
    private lateinit var estado: TextView
    private lateinit var accesibilidad: Button
    private val principal = Handler(Looper.getMainLooper())
    private val actualizar = object : Runnable {
        override fun run() {
            actualizarEstado()
            principal.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val columna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        estado = TextView(this)
        columna.addView(estado)
        columna.addView(boton(R.string.duplicar) { solicitarCaptura() })
        columna.addView(boton(R.string.detener) { stopService(Intent(this, ServicioEspejo::class.java)) })
        accesibilidad = boton(R.string.activar_toques) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        columna.addView(accesibilidad)
        setContentView(columna)
        pedirNotificaciones()
    }

    override fun onResume() {
        super.onResume()
        principal.post(actualizar)
    }

    override fun onPause() {
        principal.removeCallbacks(actualizar)
        super.onPause()
    }

    @Deprecated("Contrato de resultado de Activity sin dependencias AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SOLICITUD_CAPTURA) return
        if (resultCode != RESULT_OK || data == null) return
        startForegroundService(Intent(this, ServicioEspejo::class.java).apply {
            putExtra(ServicioEspejo.RESULTADO_CODIGO, resultCode)
            putExtra(ServicioEspejo.RESULTADO_DATOS, data)
        })
    }

    private fun boton(texto: Int, accion: () -> Unit): Button = Button(this).apply {
        setText(texto)
        setOnClickListener { accion() }
    }

    @Suppress("DEPRECATION")
    private fun solicitarCaptura() {
        if (ServicioEspejo.activo) return
        val gestor = getSystemService(MediaProjectionManager::class.java)
        val captura = if (Build.VERSION.SDK_INT >= 34) {
            gestor.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            gestor.createScreenCaptureIntent()
        }
        startActivityForResult(captura, SOLICITUD_CAPTURA)
    }

    private fun pedirNotificaciones() {
        if (Build.VERSION.SDK_INT < 33) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), SOLICITUD_NOTIFICACIONES)
    }

    private fun actualizarEstado() {
        val toquesActivos = ServicioToques.activo != null
        estado.text = getString(R.string.estado, direccionWifi(), etiquetaEstado(toquesActivos), etiquetaEstado(ServicioEspejo.activo))
        accesibilidad.visibility = if (toquesActivos) View.GONE else View.VISIBLE
    }

    private fun etiquetaEstado(activo: Boolean): String = getString(if (activo) R.string.activo else R.string.inactivo)

    private fun direccionWifi(): String {
        val gestor = getSystemService(ConnectivityManager::class.java)
        val red = gestor.allNetworks.firstOrNull {
            gestor.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } ?: return getString(R.string.sin_wifi)
        val direccion = gestor.getLinkProperties(red)?.linkAddresses?.firstOrNull { it.address is Inet4Address }
        return direccion?.address?.hostAddress ?: getString(R.string.sin_wifi)
    }

    private companion object {
        const val SOLICITUD_CAPTURA = 1
        const val SOLICITUD_NOTIFICACIONES = 2
    }
}
