package com.jarvis.assistant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.background.JarvisConversationService
import com.jarvis.assistant.ui.home.MainActivity

/** Quick voice/chat/standby controls for the Android home-screen widget family. */
class HelloAiControlWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, views(context, id)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TALK -> ContextCompat.startForegroundService(
                context,
                Intent(context, JarvisConversationService::class.java).setAction(JarvisConversationService.ACTION_START_CONVERSATION)
            )
            ACTION_STANDBY -> ContextCompat.startForegroundService(
                context,
                Intent(context, JarvisConversationService::class.java).setAction(JarvisConversationService.ACTION_ENTER_STANDBY)
            )
            ACTION_CHAT -> context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(MainActivity.EXTRA_OPEN_CHAT, true)
            )
        }
        super.onReceive(context, intent)
    }

    private fun views(context: Context, id: Int): RemoteViews = RemoteViews(context.packageName, R.layout.widget_helloai_control).apply {
        setOnClickPendingIntent(R.id.widgetControlTalk, command(context, id, ACTION_TALK))
        setOnClickPendingIntent(R.id.widgetControlChat, command(context, id, ACTION_CHAT))
        setOnClickPendingIntent(R.id.widgetControlStandby, command(context, id, ACTION_STANDBY))
    }

    private fun command(context: Context, id: Int, action: String): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, id + action.hashCode(), Intent(context, HelloAiControlWidgetProvider::class.java).setAction(action), flags)
    }

    companion object {
        private const val ACTION_TALK = "com.myappai.app.widget.TALK"
        private const val ACTION_CHAT = "com.myappai.app.widget.CHAT"
        private const val ACTION_STANDBY = "com.myappai.app.widget.STANDBY"
    }
}
