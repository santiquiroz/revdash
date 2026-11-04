package com.revdash.espejo

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder

class ServicioEspejo : Service() {
    private var sesion: SesionEspejo? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (sesion != null) return START_NOT_STICKY
        val permiso = intent?.getParcelableExtra<Intent>(RESULTADO_DATOS)
        val resultado = intent?.getIntExtra(RESULTADO_CODIGO, Activity.RESULT_CANCELED)
        if (permiso == null || resultado != Activity.RESULT_OK) {
            stopSelf()
            return START_NOT_STICKY
        }
        iniciarNotificacion()
        activo = true
        sesion = SesionEspejo(this, resultado, permiso) { stopSelf() }.also { it.iniciar() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        activo = false
        sesion?.detener()
        sesion = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun iniciarNotificacion() {
        val gestor = getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(NotificationChannel(CANAL, getString(R.string.canal_captura), NotificationManager.IMPORTANCE_LOW))
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notificacion = Notification.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.captura_activa))
            .setContentIntent(abrir)
            .setOngoing(true)
            .build()
        startForeground(1, notificacion, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    companion object {
        const val RESULTADO_CODIGO = "resultado"
        const val RESULTADO_DATOS = "permiso"
        private const val CANAL = "captura"
        @Volatile var activo = false
            private set
    }
}
