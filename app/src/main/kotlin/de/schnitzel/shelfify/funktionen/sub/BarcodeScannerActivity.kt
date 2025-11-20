package de.schnitzel.shelfify.funktionen.sub

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import de.schnitzel.shelfify.R

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private var cameraProvider: ProcessCameraProvider? = null
    private val scanner = BarcodeScanning.getClient()
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null

    private lateinit var btnFlash: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)

        previewView = findViewById(R.id.previewView)
        btnFlash = findViewById(R.id.btnFlashlight)

        btnFlash.setOnClickListener { toggleFlashlight() }
        btnFlash.isEnabled = false // deaktiviert, bis Kamera läuft

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun toggleFlashlight() {
        camera?.let { cam ->
            val cameraInfo = cam.cameraInfo
            val cameraControl = cam.cameraControl

            val torchState = cameraInfo.torchState.value
            if (torchState != null) {
                val isTorchOn = torchState == TorchState.ON
                cameraControl.enableTorch(!isTorchOn)
                btnFlash.text = if (!isTorchOn) "🔦 Licht aus" else "🔦 Licht an"
            } else {
                Toast.makeText(this, "Taschenlampenstatus unbekannt", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Kamera nicht bereit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindPreviewAndAnalyzer()
            } catch (e: Exception) {
                Log.e("BarcodeScanner", e.stackTrace.toString())
                Toast.makeText(this, "Kamera konnte nicht gestartet werden", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreviewAndAnalyzer() {
        val provider = cameraProvider ?: return

        try {
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            // Vorherige Bindungen aufheben
            provider.unbindAll()

            // Preview einrichten
            preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }

            // Bildanalyse einrichten
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis?.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                processImageProxy(imageProxy)
            }

            // Kamera an Lebenszyklus binden
            camera = provider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )

            // Taschenlampen-Button aktivieren
            btnFlash.isEnabled = true
            btnFlash.text = if (camera?.cameraInfo?.torchState?.value == TorchState.ON)
                "🔦 Licht aus" else "🔦 Licht an"

        } catch (e: Exception) {
            Log.e("BarcodeScanner", e.stackTrace.toString())
            Toast.makeText(this, "Kamera konnte nicht initialisiert werden", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode: Barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null) {
                            cleanupAndFinish(rawValue)
                            imageProxy.close()
                            return@addOnSuccessListener
                        }
                    }
                    imageProxy.close()
                }
                .addOnFailureListener { exception ->
                    imageProxy.close()
                    Log.e("BarcodeScanner", exception.stackTrace.toString())
                    Toast.makeText(this, "Fehler beim Scannen", Toast.LENGTH_SHORT).show()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun cleanupAndFinish(barcode: String) {
        imageAnalysis?.clearAnalyzer()
        preview?.surfaceProvider = null
        cameraProvider?.unbindAll()
        scanner.close()

        val resultIntent = Intent().apply {
            putExtra("ean", barcode)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Kamerazugriff erforderlich", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        imageAnalysis?.clearAnalyzer()
        preview?.surfaceProvider = null
        scanner.close()
    }
}