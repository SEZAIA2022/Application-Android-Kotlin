package com.houssein.sezaia.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.ScaleGestureDetector
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.houssein.sezaia.R
import com.houssein.sezaia.model.request.QrCodeRequest
import com.houssein.sezaia.model.response.QrCodeResponse
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.BarcodeOverlayView
import com.houssein.sezaia.ui.utils.UIUtils
import com.houssein.sezaia.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: BarcodeOverlayView
    private val executor = Executors.newSingleThreadExecutor()
    private val CAMERA_PERMISSION_CODE = 1001
    private var isQrCodeProcessed = false
    private var currentFlashMode = FlashMode.OFF
    private lateinit var flashButton: ImageButton
    private var isAutoFlashEnabled = false
    private lateinit var camera: Camera
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var currentZoomRatio = 1f




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)
        flashButton = findViewById(R.id.flashButton)
        flashButton.setOnClickListener {
            toggleFlash()
        }

        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        UIUtils.initToolbar(
            this,getString(R.string.qr_code_scan),actionIconRes = R.drawable.baseline_density_medium_24, onBackClick = {},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!::camera.isInitialized) return false

                val scale = detector.scaleFactor
                currentZoomRatio = (currentZoomRatio * scale).coerceIn(
                    camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1f,
                    camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 10f
                )
                camera.cameraControl.setZoomRatio(currentZoomRatio)
                return true
            }
        })


        requestCameraPermission()
    }

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        event?.let {
            scaleGestureDetector.onTouchEvent(it)
        }
        return super.onTouchEvent(event)
    }

    enum class FlashMode {
        OFF, ON, AUTO
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val barcodeScanner = BarcodeScanning.getClient()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executor) { imageProxy -> processImageProxy(barcodeScanner, imageProxy) }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // Affecter la caméra ici !
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Échec de la liaison de la caméra", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlash() {
        if (!::camera.isInitialized || !camera.cameraInfo.hasFlashUnit()) {
            Toast.makeText(this, "La torche n'est pas disponible sur cet appareil", Toast.LENGTH_SHORT).show()
            return
        }

        currentFlashMode = when (currentFlashMode) {
            FlashMode.OFF -> {
                camera.cameraControl.enableTorch(true)
                flashButton.setImageResource(R.drawable.baseline_flash_on_24) // icône flash ON
                isAutoFlashEnabled = false
                FlashMode.ON
            }

            FlashMode.ON -> {
                camera.cameraControl.enableTorch(false)
                flashButton.setImageResource(R.drawable.baseline_flash_auto_24) // icône flash AUTO
                isAutoFlashEnabled = true
                FlashMode.AUTO
            }

            FlashMode.AUTO -> {
                camera.cameraControl.enableTorch(false)
                flashButton.setImageResource(R.drawable.baseline_flash_off_24) // icône flash OFF
                isAutoFlashEnabled = false
                FlashMode.OFF
            }
        }
    }
    private fun calculateLuminance(image: Image): Double {
        val buffer = image.planes[0].buffer
        var sum = 0
        while (buffer.hasRemaining()) {
            sum += buffer.get().toInt() and 0xFF
        }
        return sum.toDouble() / buffer.capacity()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(scanner: com.google.mlkit.vision.barcode.BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        val luminance = calculateLuminance(mediaImage)
        if (isAutoFlashEnabled) {
            val lowLight = luminance < 50  // seuil ajustable
            if (::camera.isInitialized && camera.cameraInfo.hasFlashUnit()) {
                camera.cameraControl.enableTorch(lowLight)
            }
        }

        if (isQrCodeProcessed) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val prefs = getSharedPreferences("LoginData", MODE_PRIVATE)
        val username = prefs.getString("loggedUsername", null)
        val role = prefs.getString("userRole", null)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach { barcode ->
                    barcode.rawValue?.let { value ->
                        barcode.boundingBox?.let { box ->
                            processBoundingBox(box, imageProxy, value, username.toString(), role.toString())
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Échec du scan du QR code", Toast.LENGTH_SHORT).show()
                overlayView.clearBox()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processBoundingBox(box: Rect, imageProxy: ImageProxy, value: String, username: String, role: String) {
        val previewWidth = previewView.width.toFloat()
        val previewHeight = previewView.height.toFloat()
        val imageWidth = imageProxy.image?.width?.toFloat() ?: 0f
        val imageHeight = imageProxy.image?.height?.toFloat() ?: 0f
        val rotation = imageProxy.imageInfo.rotationDegrees

        val (scaleX, scaleY) = if (rotation == 90 || rotation == 270) {
            previewWidth / imageHeight to previewHeight / imageWidth
        } else {
            previewWidth / imageWidth to previewHeight / imageHeight
        }

        val scaledBox = RectF(
            box.left * scaleX,
            box.top * scaleY,
            box.right * scaleX,
            box.bottom * scaleY
        )

        overlayView.setBoundingBox(scaledBox)

        if (!isQrCodeProcessed) {
            isQrCodeProcessed = true
            verifyQrCodeWithServer(value, username, role)
        }
    }

    private fun verifyQrCodeWithServer(qrCode: String, username: String, role: String) {
        RetrofitClient.instance.checkQrCode(QrCodeRequest(qrCode, username, role))
            .enqueue(object : Callback<QrCodeResponse> {
                override fun onResponse(call: Call<QrCodeResponse>, response: Response<QrCodeResponse>) {
                    val body = response.body()
                    val status_repair = body?.status_repair.toString()
                    val id_repair_ask = body?.id_ask_repair.toString()
                    val message = body?.message.toString()
                    if (response.isSuccessful && body?.status == "success") {
                        when (body.is_active) {
                            true -> handleActiveQrCode(qrCode, role, status_repair, id_repair_ask, message)
                            false -> handleInactiveQrCode(qrCode, message)
                            else -> {
                                Log.w("QRCode", "QR code unknown state or null")
                                Toast.makeText(this@CameraActivity, "QR code unknown state", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val errorMessage = body?.message ?: "QR code is not valid."
                        Toast.makeText(this@CameraActivity, errorMessage, Toast.LENGTH_SHORT).show()
                        resetScannerWithDelay()
                    }
                }

                override fun onFailure(call: Call<QrCodeResponse>, t: Throwable) {
                    Toast.makeText(this@CameraActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    resetScannerWithDelay()
                }
            })
    }

    // Gère les QR codes actifs
    private fun handleActiveQrCode(qrCode: String, role: String, status_repair: String, id_repair_ask: String, message: String) {

        if (role == "admin") {
            if (status_repair == "processing"){
                saveQrCode(qrCode)
                getSharedPreferences("MyPrefs", MODE_PRIVATE).edit().apply {
                    putString("id", id_repair_ask)
                    putString("status_repair", status_repair)
                    apply()
                }
                startActivity(Intent(this@CameraActivity, RepairActivity::class.java))
            } else {
                Toast.makeText(this@CameraActivity, message, Toast.LENGTH_SHORT).show()
                resetScannerWithDelay()
            }

        } else {
            saveQrCode(qrCode)
            startActivity(Intent(this@CameraActivity, WelcomeChatbotActivity::class.java))
        }
    }

    // Gère les QR codes inactifs
    private fun handleInactiveQrCode(qrCode: String, message: String) {
        saveQrCode(qrCode)
        startActivity(Intent(this@CameraActivity, ActivateQrCodeActivity::class.java))
    }

    // Sauvegarde des données dans SharedPreferences
    private fun saveQrCode(qrCode: String) {
        getSharedPreferences("MyPrefs", MODE_PRIVATE).edit().apply {
            putString("qrData", qrCode)
            apply()
        }
    }

    // Réinitialisation du scanner après 3 secondes
    private fun resetScannerWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            isQrCodeProcessed = false
            overlayView.clearBox()
        }, 3000)
    }

    override fun onResume() {
        super.onResume()
        isQrCodeProcessed = false
        if (::overlayView.isInitialized) {
            overlayView.clearBox()
        }
        requestCameraPermission()
    }
}
