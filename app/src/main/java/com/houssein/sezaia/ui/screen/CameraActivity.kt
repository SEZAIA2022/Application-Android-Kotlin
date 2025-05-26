package com.houssein.sezaia.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Size
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
import com.houssein.sezaia.ui.utils.BarcodeOverlayView
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: BarcodeOverlayView
    private val executor = Executors.newSingleThreadExecutor()
    private val CAMERA_PERMISSION_CODE = 1001
    private var isQrCodeProcessed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_camera)

        // Appliquer les insets des barres système
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("showCardsInSettings", true).apply()

        UIUtils.initToolbar(
            this,getString(R.string.welcome_user), actionIconRes = R.drawable.baseline_density_medium_24, onBackClick = {finish()},
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)

        requestCameraPermission()
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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Échec de la liaison de la caméra", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(scanner: com.google.mlkit.vision.barcode.BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        if (isQrCodeProcessed) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach { barcode ->
                    barcode.rawValue?.let { value ->
                        barcode.boundingBox?.let { box ->
                            processBoundingBox(box, imageProxy, value)
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
    private fun processBoundingBox(box: Rect, imageProxy: ImageProxy, value: String) {
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
            Toast.makeText(this, "QR Code scanné: $value", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, WelcomeChatbotActivity::class.java)
            intent.putExtra("qr_data", value)
            startActivity(intent)
        }
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
