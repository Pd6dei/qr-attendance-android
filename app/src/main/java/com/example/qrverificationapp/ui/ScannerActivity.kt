package com.example.qrverificationapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.qrverificationapp.R
import com.example.qrverificationapp.data.QrRequest
import com.example.qrverificationapp.data.RetrofitClient
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var userName: String

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val scannedOnce = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null

    companion object {
        private const val CAMERA_PERMISSION_CODE = 101
        private const val TAG = "ScannerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        previewView = findViewById(R.id.previewView)

        userName = intent.getStringExtra("USER_NAME") ?: ""

        Log.d(TAG, "Logged-in user = $userName")

        if (userName.isBlank()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        scannedOnce.set(false)
        if (hasCameraPermission() && cameraProvider == null) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /* ================= CAMERA PERMISSION ================= */

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    /* ================= CAMERA + QR SCAN ================= */

    private fun startCamera() {

        Log.d(TAG, "Starting camera")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()
            cameraProvider?.unbindAll()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val barcodeScanner = BarcodeScanning.getClient()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

                if (scannedOnce.get()) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mediaImage = imageProxy.image
                if (mediaImage != null) {

                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->

                            val qrValue = barcodes.firstOrNull()?.rawValue

                            if (!qrValue.isNullOrBlank() &&
                                scannedOnce.compareAndSet(false, true)
                            ) {
                                Log.d(TAG, "QR Scanned = $qrValue")
                                fetchStudent(qrValue.trim())
                            }
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "QR scan failed", it)
                            scannedOnce.set(false)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            cameraProvider?.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    /* ================= API CALL ================= */

    private fun fetchStudent(qrData: String) {

        Log.d(TAG, "Calling fetchStudent API")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = QrRequest(application_number = qrData)
                val response = RetrofitClient.api.fetchStudent(request)

                Log.d(TAG, "API status = ${response.status}")
                Log.d(TAG, "PHOTO URL = ${response.photo_url}")
                Log.d(TAG, "SIGNATURE URL = ${response.signature_url}")

                withContext(Dispatchers.Main) {

                    if (response.status == "OK") {

                        val intent =
                            Intent(this@ScannerActivity, VerifyActivity::class.java)

                        intent.putExtra("APPLICATION_NO", response.application_number)
                        intent.putExtra("PROGRAM_ID", response.program_id)
                        intent.putExtra("PROGRAM_NAME", response.program_name)
                        intent.putExtra("STUDENT_NAME", response.student_name)
                        intent.putExtra("DATE_OF_BIRTH", response.dob)
                        intent.putExtra("PHOTO", response.photo_url)
                        intent.putExtra("SIGNATURE", response.signature_url)
                        intent.putExtra("USER_NAME", userName)

                        Log.d(TAG, "Opening VerifyActivity")
                        startActivity(intent)

                    } else {
                        Toast.makeText(
                            this@ScannerActivity,
                            response.message ?: "Invalid QR",
                            Toast.LENGTH_LONG
                        ).show()
                        scannedOnce.set(false)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ScannerActivity,
                        "Server not reachable",
                        Toast.LENGTH_LONG
                    ).show()
                    scannedOnce.set(false)
                }
            }
        }
    }
}
