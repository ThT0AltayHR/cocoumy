package com.jarvis.assistant.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/** Routes a live assistant conversation to the phone earpiece or loudspeaker when supported. */
object AudioRouteController {
    enum class Route { SPEAKER, EARPIECE }

    fun setRoute(context: Context, route: Route): Boolean {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return runCatching {
            manager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val type = if (route == Route.SPEAKER) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                val device = manager.availableCommunicationDevices.firstOrNull { it.type == type }
                    ?: return@runCatching false
                manager.setCommunicationDevice(device)
            } else {
                @Suppress("DEPRECATION")
                run { manager.isSpeakerphoneOn = route == Route.SPEAKER }
                true
            }
        }.getOrDefault(false)
    }
}
