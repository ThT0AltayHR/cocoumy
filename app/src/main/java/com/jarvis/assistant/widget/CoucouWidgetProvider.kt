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

/** Minimal home-screen widget. It never stores credentials or starts microphone capture by itself. */
class CoucouWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(context, manager, it, lastStatus.ifBlank { "Hazır · Sohbeti aç" }) }
    }

    companion object {
        @Volatile private var lastStatus: String = "Hazır · Sohbeti aç"

        fun updateStatus(context: Context, status: String) {
            lastStatus = status
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, CoucouWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateOne(context, manager, it, status) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, id: Int, status: String) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_OPEN_CHAT, true)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (android.os.Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
            val pending = PendingIntent.getActivity(context, id, openIntent, flags)
            val views = RemoteViews(context.packageName, R.layout.widget_coucou_status).apply {
                setTextViewText(R.id.widgetStatus, status)
                setOnClickPendingIntent(R.id.widgetRoot, pending)
                setOnClickPendingIntent(R.id.widgetOpenChat, pending)
            }
            manager.updateAppWidget(id, views)
        }
    }
}
