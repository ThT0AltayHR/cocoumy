package com.jarvis.assistant.ui.island

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import com.jarvis.assistant.background.JarvisConversationService
import com.jarvis.assistant.data.model.ConversationState
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.ui.home.MainActivity
import kotlin.math.max
import kotlin.math.min

/**
 * Android-native, Dynamic-Island-inspired assistant pill. It is deliberately a
 * normal Android overlay rather than a privileged system UI replacement.
 * Android's status bar/cutout is owned by the OS; showing this pill requires
 * the user's explicit "Display over other apps" permission.
 */
class DynamicIslandOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()

    private val shell = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(26).toFloat()
            setColor(Color.argb(242, 12, 14, 20))
            setStroke(dp(1), Color.argb(110, 150, 165, 190))
        }
        elevation = dp(16).toFloat()
        clipToPadding = false
        clipChildren = false
        isClickable = true
        contentDescription = "HelloAI canlı asistan alanı"
    }

    private val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val mood = ImageView(context).apply {
        setImageResource(com.jarvis.assistant.R.drawable.ic_helloai_emote)
        gravity = Gravity.CENTER
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }
    private val titleColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(9), 0, dp(4), 0)
    }
    private val title = TextView(context).apply {
        text = "HELLOAI"
        textSize = 11f
        setTextColor(Color.rgb(245, 247, 252))
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        letterSpacing = .08f
        maxLines = 1
    }
    private val stateLabel = TextView(context).apply {
        text = "HAZIR"
        textSize = 9f
        setTextColor(Color.rgb(165, 181, 206))
        setPadding(0, dp(2), 0, 0)
        maxLines = 1
    }
    private val expandButton = TextView(context).apply {
        text = "⌄"
        textSize = 20f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(196, 205, 222))
        setPadding(dp(10), 0, 0, 0)
        contentDescription = "Asistan alanını genişlet"
    }
    private val message = TextView(context).apply {
        text = "Buradayım, seni dinliyorum."
        textSize = 12f
        setTextColor(Color.rgb(221, 226, 237))
        setPadding(dp(1), dp(8), dp(1), dp(2))
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private val userLine = TextView(context).apply {
        textSize = 10f
        setTextColor(Color.rgb(142, 153, 177))
        setPadding(dp(1), dp(4), dp(1), 0)
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
        visibility = View.GONE
    }
    private val actionRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(9), 0, 0)
        visibility = View.GONE
    }
    private val replyButton = actionButton("Sohbete dön", true)
    private val stopButton = actionButton("Canlıyı kapat", false)
    private val listenButton = actionButton("Sesli sohbet", false)

    private val params = WindowManager.LayoutParams(
        min(dp(JarvisApp.instance.preferences.dynamicIslandWidthDp), context.resources.displayMetrics.widthPixels - dp(20)),
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        android.graphics.PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = statusBarHeight() + dp(JarvisApp.instance.preferences.dynamicIslandTopDp)
    }

    private var attached = false
    private var expanded = false
    private var pulseAnimator: AnimatorSet? = null
    private var lastAssistantText = ""

    init {
        headerRow.addView(mood, LinearLayout.LayoutParams(dp(28), dp(28)))
        titleColumn.addView(title)
        titleColumn.addView(stateLabel)
        headerRow.addView(titleColumn, LinearLayout.LayoutParams(0, -2, 1f))
        headerRow.addView(expandButton, LinearLayout.LayoutParams(-2, dp(30)))
        shell.addView(headerRow, LinearLayout.LayoutParams(-1, -2))
        shell.addView(message, LinearLayout.LayoutParams(-1, -2))
        shell.addView(userLine, LinearLayout.LayoutParams(-1, -2))
        actionRow.addView(replyButton, LinearLayout.LayoutParams(0, dp(34), 1f))
        actionRow.addView(View(context), LinearLayout.LayoutParams(dp(6), 1))
        actionRow.addView(listenButton, LinearLayout.LayoutParams(0, dp(34), 1f))
        actionRow.addView(View(context), LinearLayout.LayoutParams(dp(6), 1))
        actionRow.addView(stopButton, LinearLayout.LayoutParams(0, dp(34), 1f))
        shell.addView(actionRow, LinearLayout.LayoutParams(-1, -2))

        shell.setOnClickListener { setExpanded(!expanded) }
        expandButton.setOnClickListener { setExpanded(!expanded) }
        replyButton.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_OPEN_CHAT, true)
            }
            context.startActivity(intent)
        }
        listenButton.setOnClickListener {
            context.startService(Intent(context, JarvisConversationService::class.java).setAction(JarvisConversationService.ACTION_START_CONVERSATION))
            setExpanded(false)
        }
        stopButton.setOnClickListener {
            context.startService(Intent(context, JarvisConversationService::class.java).setAction(JarvisConversationService.ACTION_STOP_ALL))
            remove()
        }
    }

    fun update(
        state: ConversationState,
        isSessionOn: Boolean,
        isStandby: Boolean,
        userText: String,
        assistantText: String,
        enabled: Boolean
    ) {
        if (!enabled || !JarvisApp.instance.preferences.isDynamicIslandEnabled || !Settings.canDrawOverlays(context)) {
            remove()
            return
        }
        params.width = min(dp(JarvisApp.instance.preferences.dynamicIslandWidthDp), context.resources.displayMetrics.widthPixels - dp(20))
        params.y = statusBarHeight() + dp(JarvisApp.instance.preferences.dynamicIslandTopDp)
        if (!attached) {
            try {
                windowManager.addView(shell, params)
                attached = true
            } catch (_: Exception) {
                attached = false
                return
            }
        }

        val presentation = presentationFor(state, isSessionOn, isStandby, userText, assistantText)
        if (presentation.view in setOf(IslandView.APPROVAL, IslandView.QUESTION, IslandView.ERROR)) {
            expanded = true
        }
        applySize()
        val details = Triple("●", presentation.title, presentation.message)
        mood.setImageResource(emoteResource(presentation.view, isSessionOn))
        mood.contentDescription = "HelloAI ${presentation.emote.assetName} emote"
        mood.imageTintList = android.content.res.ColorStateList.valueOf(
            islandAccent(presentation.view, isSessionOn)
        )
        stateLabel.text = details.second
        stateLabel.setTextColor(islandAccent(presentation.view, isSessionOn))
        updatePulse(state == ConversationState.THINKING || state == ConversationState.LISTENING || expanded)
        val latest = assistantText.trim().ifBlank { details.third }
        lastAssistantText = latest
        message.text = latest.take(260)
        userLine.text = if (userText.isBlank()) "" else "Sen: ${userText.trim().take(180)}"
        userLine.visibility = if (expanded && userText.isNotBlank()) View.VISIBLE else View.GONE
        message.maxLines = if (expanded) 6 else 2
        actionRow.visibility = if (expanded) View.VISIBLE else View.GONE
        expandButton.text = if (expanded) "⌃" else "⌄"
        try { windowManager.updateViewLayout(shell, params) } catch (_: Exception) {}
    }

    private fun emoteResource(view: IslandView, isSessionOn: Boolean): Int {
        if (!isSessionOn) return com.jarvis.assistant.R.drawable.ic_helloai_error
        return when (view) {
            IslandView.LISTENING -> com.jarvis.assistant.R.drawable.ic_helloai_listening
            IslandView.THINKING, IslandView.SEARCHING -> com.jarvis.assistant.R.drawable.ic_helloai_thinking
            IslandView.ERROR -> com.jarvis.assistant.R.drawable.ic_helloai_error
            IslandView.FINISHED, IslandView.SPEAKING -> com.jarvis.assistant.R.drawable.ic_helloai_finished
            else -> com.jarvis.assistant.R.drawable.ic_helloai_emote
        }
    }

    private fun islandAccent(view: IslandView, isSessionOn: Boolean): Int {
        if (!isSessionOn) return Color.rgb(236, 80, 88)
        return when (view) {
            IslandView.ERROR -> Color.rgb(236, 80, 88)
            IslandView.APPROVAL, IslandView.QUESTION -> Color.rgb(249, 190, 64)
            IslandView.SEARCHING, IslandView.THINKING -> Color.rgb(118, 163, 255)
            IslandView.FINISHED -> Color.rgb(74, 230, 142)
            else -> Color.rgb(74, 230, 142)
        }
    }

    private fun presentationFor(
        state: ConversationState,
        isSessionOn: Boolean,
        isStandby: Boolean,
        userText: String,
        assistantText: String
    ): IslandPresentation {
        if (isStandby && !isSessionOn) {
            return IslandPresentation(IslandMode.COMPACT, IslandView.GREETING, IslandEmote.CALM,
                "HEY JARVIS BEKLENİYOR", "Uyandırma sözcüğünü bekliyorum.")
        }
        if (!isSessionOn) {
            return IslandPresentation(IslandMode.COMPACT, IslandView.EMPTY, IslandEmote.TIRED,
                "ÇEVRİMDIŞI", "Canlı asistan kapalı.")
        }
        return when (IslandStateMachine.classify(state, userText, assistantText)) {
            IslandView.APPROVAL -> IslandPresentation(IslandMode.EXPANDED, IslandView.APPROVAL, IslandEmote.QUESTION,
                "ONAY GEREKİYOR", assistantText.ifBlank { "Bu işlem için onayını bekliyorum." })
            IslandView.ERROR -> IslandPresentation(IslandMode.EXPANDED, IslandView.ERROR, IslandEmote.ERROR,
                "İŞLEMDE HATA", assistantText.ifBlank { "İşlem tamamlanamadı." })
            IslandView.QUESTION -> IslandPresentation(IslandMode.EXPANDED, IslandView.QUESTION, IslandEmote.QUESTION,
                "SEÇİM GEREKİYOR", assistantText.ifBlank { "Nasıl devam edelim?" })
            IslandView.SEARCHING -> IslandPresentation(IslandMode.EXPANDED, IslandView.SEARCHING, IslandEmote.THINKING,
                "ARANIYOR", assistantText.ifBlank { "Sonuçlar inceleniyor…" })
            IslandView.LISTENING -> IslandPresentation(IslandMode.EXPANDED, IslandView.LISTENING, IslandEmote.LISTENING,
                "CANLI SOHBET", userText.ifBlank { "Seni dinliyorum." })
            IslandView.THINKING -> IslandPresentation(IslandMode.EXPANDED, IslandView.THINKING, IslandEmote.THINKING,
                "DÜŞÜNÜYOR", assistantText.ifBlank { "İsteğini değerlendiriyorum…" })
            IslandView.SPEAKING -> IslandPresentation(IslandMode.EXPANDED, IslandView.SPEAKING, IslandEmote.PROUD,
                "YANITLIYOR", assistantText.ifBlank { "Yanıt hazırlanıyor…" })
            IslandView.FINISHED -> IslandPresentation(IslandMode.COMPACT, IslandView.FINISHED, IslandEmote.FINISHED,
                "TAMAMLANDI", assistantText.takeIf { it.isNotBlank() } ?: "İşlem tamamlandı.")
            else -> IslandPresentation(IslandMode.COMPACT, IslandView.OVERVIEW, IslandEmote.CALM,
                "HAZIR", "Bağlantı hazır.")
        }
    }

    private fun updatePulse(shouldPulse: Boolean) {
        if (shouldPulse && pulseAnimator == null) {
            val scaleX = ObjectAnimator.ofFloat(mood, View.SCALE_X, 1f, 1.12f, 1f).apply { duration = 1150L; repeatCount = ObjectAnimator.INFINITE; interpolator = AccelerateDecelerateInterpolator() }
            val scaleY = ObjectAnimator.ofFloat(mood, View.SCALE_Y, 1f, 1.12f, 1f).apply { duration = 1150L; repeatCount = ObjectAnimator.INFINITE; interpolator = AccelerateDecelerateInterpolator() }
            pulseAnimator = AnimatorSet().apply { playTogether(scaleX, scaleY); start() }
        } else if (!shouldPulse) {
            pulseAnimator?.cancel()
            pulseAnimator = null
            mood.scaleX = 1f
            mood.scaleY = 1f
        }
    }

    fun remove() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        if (attached) {
            try { windowManager.removeView(shell) } catch (_: Exception) {}
            attached = false
        }
    }

    private fun setExpanded(value: Boolean) {
        expanded = value
        userLine.visibility = if (expanded) View.VISIBLE else View.GONE
        actionRow.visibility = if (expanded) View.VISIBLE else View.GONE
        message.maxLines = if (expanded) 6 else 2
        expandButton.text = if (expanded) "⌃" else "⌄"
        applySize()
        try { if (attached) windowManager.updateViewLayout(shell, params) } catch (_: Exception) {}
    }

    private fun applySize() {
        val compactHeight = max(dp(JarvisApp.instance.preferences.dynamicIslandHeightDp), dp(52))
        shell.minimumHeight = if (expanded) 0 else compactHeight
        params.height = if (expanded) WindowManager.LayoutParams.WRAP_CONTENT else compactHeight
    }

    private fun actionButton(label: String, primary: Boolean) = TextView(context).apply {
        text = label
        textSize = 11f
        gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setTextColor(if (primary) Color.rgb(14, 18, 27) else Color.rgb(238, 241, 248))
        background = GradientDrawable().apply {
            cornerRadius = dp(17).toFloat()
            setColor(if (primary) Color.rgb(215, 225, 245) else Color.argb(90, 112, 125, 150))
            if (!primary) setStroke(dp(1), Color.argb(95, 175, 188, 208))
        }
        isClickable = true
        isFocusable = true
    }

    private fun statusBarHeight(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else dp(24)
    }
}
