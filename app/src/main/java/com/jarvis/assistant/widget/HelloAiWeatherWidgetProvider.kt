package com.jarvis.assistant.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jarvis.assistant.R
import com.jarvis.assistant.util.LocationWeatherHelper
import com.jarvis.assistant.ui.home.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Dedicated weather widget from the macOS widget family, backed by device location. */
class HelloAiWeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val weather = LocationWeatherHelper.fetchWeather(context)
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_helloai_weather)
                if (weather == null) {
                    views.setTextViewText(R.id.weatherTemperature, "--°C")
                    views.setTextViewText(R.id.weatherCondition, "Konum izni gerekli")
                    views.setTextViewText(R.id.weatherDetails, "HelloAI ayarlarından konum iznini aç")
                } else {
                    views.setTextViewText(R.id.weatherTemperature, weather.temperature)
                    views.setTextViewText(R.id.weatherCondition, weather.condition)
                    views.setTextViewText(R.id.weatherDetails, weather.details)
                }
                val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (android.os.Build.VERSION.SDK_INT >= 23) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
                views.setOnClickPendingIntent(id, android.app.PendingIntent.getActivity(context, id, intent, flags))
                manager.updateAppWidget(id, views)
            }
            pending.finish()
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, HelloAiWeatherWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) HelloAiWeatherWidgetProvider().onUpdate(context, manager, ids)
        }
    }
}
