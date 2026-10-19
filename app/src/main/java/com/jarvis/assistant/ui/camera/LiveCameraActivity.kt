package com.jarvis.assistant.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.R
import com.jarvis.assistant.audio.AudioRouteController
import com.jarvis.assistant.background.JarvisConversationService
import com.jarvis.assistant.ui.settings.ApiConnectionActivity
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** User-visible front/rear camera stream to the active Gemini Live session. */
class LiveCameraActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var status: TextView
    private lateinit var switchCameraButton: Button
    private lateinit var routeButton: Button
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var lensFront = false
    private var audioRoute = AudioRouteController.Route.SPEAKER
    private val lastSentAt = AtomicLong(0L)
    private var permissionRequested = false

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else {
            status.text = "Kamera izni verilmedi. Canlı görme kapalı."
            Toast.makeText(this, "Canlı görme için kamera izni gerekiyor.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!JarvisConversationService.isSessionOn.value || JarvisConversationService.connectionStatus.value != "LIVE") {
            MaterialAlertDialogBuilder(this)
                .setTitle("Önce canlı sohbeti başlat")
                .setMessage("Gemini Live bağlantısı kurulunca kamera görüntüsünü paylaşabilirsin.")
                .setPositiveButton("Gemini ayarları") { _, _ -> startActivity(android.content.Intent(this, ApiConnectionActivity::class.java)); finish() }
                .setNegativeButton("Geri dön") { _, _ -> finish() }
                .show()
            return
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        buildUi()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else { permissionRequested = true; cameraPermission.launch(Manifest.permission.CAMERA) }
        JarvisConversationService.sendTextMessage("Canlı kamera modu açıldı. Ön/arka kameradan gelen güncel görüntü karelerini yorumla; yalnızca gerçekten gördüğün şeyleri söyle ve belirsiz olanları açıkça belirt.")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(18))
            setBackgroundColor(android.graphics.Color.rgb(10, 12, 18))
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val back = button("‹ Geri") { finish() }
        val heading = TextView(this).apply {
            text = "Canlı görme"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        top.addView(back, LinearLayout.LayoutParams(dp(88), dp(44)))
        top.addView(heading, LinearLayout.LayoutParams(0, dp(44), 1f))
        top.addView(View(this), LinearLayout.LayoutParams(dp(88), dp(44)))
        root.addView(top)
        status = TextView(this).apply {
            text = "Gemini Live hazır • Kameran seçili olduğunda kareler gönderilir"
            textSize = 12f
            setTextColor(android.graphics.Color.rgb(171, 186, 211))
            setPadding(dp(2), dp(12), dp(2), dp(10))
        }
        root.addView(status)
        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setBackgroundColor(android.graphics.Color.rgb(20, 24, 35))
        }
        root.addView(previewView, LinearLayout.LayoutParams(-1, 0, 1f))
        val footnote = TextView(this).apply {
            text = "Gizlilik: Görüntü kareleri yalnızca bu ekran açıkken ve canlı oturum bağlıyken Gemini'ye gönderilir."
            textSize = 11f
            setTextColor(android.graphics.Color.rgb(145, 158, 180))
            setPadding(dp(2), dp(10), dp(2), dp(8))
        }
        root.addView(footnote)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        switchCameraButton = button("Ön kameraya geç") { switchCamera() }
        routeButton = button("Hoparlör açık") { toggleAudioRoute() }
        val close = button("Görüntüyü durdur") { cameraProvider?.unbindAll(); status.text = "Canlı görüntü durduruldu."; finish() }
        controls.addView(switchCameraButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(5) })
        controls.addView(routeButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginEnd = dp(5) })
        controls.addView(close, LinearLayout.LayoutParams(0, dp(46), 1f))
        root.addView(controls)
        setContentView(root)
    }

    private fun switchCamera() {
        lensFront = !lensFront
        cameraSelector = if (lensFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        switchCameraButton.text = if (lensFront) "Arka kameraya geç" else "Ön kameraya geç"
        startCamera()
    }

    private fun toggleAudioRoute() {
        audioRoute = if (audioRoute == AudioRouteController.Route.SPEAKER) AudioRouteController.Route.EARPIECE else AudioRouteController.Route.SPEAKER
        val routed = AudioRouteController.setRoute(this, audioRoute)
        routeButton.text = if (audioRoute == AudioRouteController.Route.SPEAKER) "Hoparlör açık" else "Ahize açık"
        if (!routed) Toast.makeText(this, "Bu cihazda seçilen ses yolu kullanılamadı.", Toast.LENGTH_SHORT).show()
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                analysis.setAnalyzer(cameraExecutor) { image ->
                    try {
                        val now = android.os.SystemClock.elapsedRealtime()
                        val prev = lastSentAt.get()
                        if (now - prev >= 1050L && lastSentAt.compareAndSet(prev, now)) {
                            val jpeg = imageToJpeg(image)
                            if (jpeg != null && JarvisConversationService.sendCameraFrame(jpeg)) {
                                runOnUiThread { status.text = if (lensFront) "Ön kamera • Gemini görüntüyü alıyor" else "Arka kamera • Gemini görüntüyü alıyor" }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("CoucouCamera", "Kare dönüştürülemedi: ${e.message}")
                    } finally { image.close() }
                }
                provider.unbindAll()
                provider.bindToLifecycle(this, cameraSelector, preview, analysis)
                status.text = if (lensFront) "Ön kamera açık • Gemini'ye yaklaşık saniyede bir kare gönderilir" else "Arka kamera açık • Gemini'ye yaklaşık saniyede bir kare gönderilir"
            } catch (e: Exception) {
                status.text = "Kamera başlatılamadı: ${e.localizedMessage ?: "bilinmeyen hata"}"
                android.util.Log.e("CoucouCamera", "Kamera başlatma hatası", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun imageToJpeg(image: ImageProxy): ByteArray? {
        if (image.format != ImageFormat.YUV_420_888 || image.planes.size < 3) return null
        val width = image.width
        val height = image.height
        val nv21 = ByteArray(width * height * 3 / 2)
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer.duplicate()
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()
        var pos = 0
        for (row in 0 until height) {
            val rowStart = row * yPlane.rowStride
            for (col in 0 until width) nv21[pos++] = yBuffer.get(rowStart + col * yPlane.pixelStride)
        }
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        for (row in 0 until chromaHeight) {
            val uRow = row * uPlane.rowStride
            val vRow = row * vPlane.rowStride
            for (col in 0 until chromaWidth) {
                nv21[pos++] = vBuffer.get(vRow + col * vPlane.pixelStride)
                nv21[pos++] = uBuffer.get(uRow + col * uPlane.pixelStride)
            }
        }
        val out = ByteArrayOutputStream()
        val ok = android.graphics.YuvImage(nv21, ImageFormat.NV21, width, height, null)
            .compressToJpeg(Rect(0, 0, width, height), 65, out)
        if (!ok) return null
        return out.toByteArray()
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 11f
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        try { cameraProvider?.unbindAll() } catch (_: Exception) {}
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdownNow()
        // Keep the conversation audio route only while actively using this screen.
        val manager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) manager.clearCommunicationDevice()
        manager.mode = AudioManager.MODE_NORMAL
        super.onDestroy()
    }
}
