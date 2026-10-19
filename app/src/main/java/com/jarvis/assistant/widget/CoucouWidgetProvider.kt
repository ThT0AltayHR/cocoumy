package com.jarvis.assistant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.home.MainActivity
import com.jarvis.assistant.util.LocationWeatherHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Minimal home-screen widget. It never stores credentials or starts microphone capture by itself. */
class CoucouWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it, lastStatus.ifBlank { "Hazır · Sohbeti aç" }, "Hava durumu yükleniyor…") }
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val weather = LocationWeatherHelper.fetchWeather(context)
            val text = weather?.let { "${it.temperature} · ${it.condition}" } ?: "Hava durumu için konum izni ver"
            ids.forEach { updateOne(context, manager, it, lastStatus, text) }
            result.finish()
        }
    }

    companion object {
        @Volatile private var lastStatus: String = "Hazır · Sohbeti aç"

        fun updateStatus(context: Context, status: String) {
            lastStatus = status
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, CoucouWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateOne(context, manager, it, status, "Hava durumu yenileniyor…") }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int, status: String, weather: String) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_OPEN_CHAT, true)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (android.os.Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
            val pending = PendingIntent.getActivity(context, id, openIntent, flags)
            val views = RemoteViews(context.packageName, R.layout.widget_coucou_status).apply {
                setTextViewText(R.id.widgetStatus, status)
                setTextViewText(R.id.widgetMood, moodForStatus(status))
                setTextViewText(R.id.widgetWeather, weather)
                setOnClickPendingIntent(R.id.widgetRoot, pending)
                setOnClickPendingIntent(R.id.widgetOpenChat, pending)
            }
            manager.updateAppWidget(id, views)
        }

        private fun moodForStatus(status: String): String {
            val normalized = status.lowercase()
            return when {
                "dinliyor" in normalized -> "Ruh hâli: dikkatli"
                "düşünüyor" in normalized || "düşün" in normalized -> "Ruh hâli: odaklı"
                "yanıt" in normalized -> "Ruh hâli: konuşuyor"
                "hata" in normalized -> "Ruh hâli: üzgün"
                else -> "Ruh hâli: sakin"
            }
        }
    }
}
