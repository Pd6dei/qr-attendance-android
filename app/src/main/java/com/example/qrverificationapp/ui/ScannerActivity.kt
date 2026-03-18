package com.example.qrverificationapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.MotionEvent
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
import com.example.qrverificationapp.data.SessionManager
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import com.example.qrverificationapp.ui.ScannerOverlayView
import android.graphics.RectF
import androidx.camera.core.FocusMeteringAction
import androidx.lifecycle.lifecycleScope

class ScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: ScannerOverlayView
    private lateinit var userName: String
    private lateinit var attendanceType: String
    private lateinit var sessionManager: SessionManager

    private var lastAnalyzedTime = 0L

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val scannedOnce = AtomicBoolean(false)

    private var cameraProvider: ProcessCameraProvider? = null
    private var boundCamera: Camera? = null

    private lateinit var scanLoader: android.widget.ProgressBar

    companion object {
        private const val CAMERA_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        scanLoader = findViewById(R.id.scanLoader)
        sessionManager = SessionManager(this)

        userName = intent.getStringExtra("USER_NAME")
            ?: sessionManager.getUser().orEmpty()

        attendanceType = intent.getStringExtra("ATTENDANCE_TYPE")
            ?: sessionManager.getAttendanceType().orEmpty()

        if (userName.isBlank() || attendanceType.isBlank()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        if (hasCameraPermission()) startCamera()
        else requestCameraPermission()

        enableTapToFocus()
    }

    override fun onResume() {
        super.onResume()

        scannedOnce.set(false)

        if (hasCameraPermission()) {
            startCamera()
        }
    }
    override fun onPause() {
        super.onPause()
        cameraProvider?.unbindAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /* ================= PERMISSION ================= */

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /* ================= CAMERA ================= */

    private fun startCamera() {

       // cameraProvider?.unbindAll()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()
            cameraProvider?.unbindAll()

            val preview = Preview.Builder()
                .setTargetResolution(Size(1280, 720))
                .build()
                .apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAllPotentialBarcodes() // 🔥 important
                .build()

            val barcodeScanner = BarcodeScanning.getClient(options)

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

                if (scannedOnce.get()) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                // 🔥 FRAME THROTTLING (ADD THIS)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAnalyzedTime < 150) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                lastAnalyzedTime = currentTime

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->

                        val boxRect = overlayView.getBoxRect()

                        // 🔥 MOVE THESE OUTSIDE LOOP (optimization)
                        val previewWidth = previewView.width
                        val previewHeight = previewView.height

                        val scaleX = previewWidth.toFloat() / image.width
                        val scaleY = previewHeight.toFloat() / image.height

                        for (barcode in barcodes) {

                            val boundingBox = barcode.boundingBox
                            if (boundingBox != null && boxRect != null) {

                              //  val previewWidth = previewView.width
                              //  val previewHeight = previewView.height

                              //  val scaleX = previewWidth.toFloat() / image.width
                              //  val scaleY = previewHeight.toFloat() / image.height

                                val scaledRect = RectF(
                                    boundingBox.left * scaleX,
                                    boundingBox.top * scaleY,
                                    boundingBox.right * scaleX,
                                    boundingBox.bottom * scaleY
                                )

                             //   if (boxRect.contains(scaledRect)) {

                                    val qrValue = barcode.rawValue

                                    if (!qrValue.isNullOrBlank() &&
                                        scannedOnce.compareAndSet(false, true)
                                    ) {

                                        fetchStudent(qrValue.trim())
                                        break
                                //    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Optional: Log error
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }

            }

            boundCamera = cameraProvider?.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

            val factory = previewView.meteringPointFactory
            val centerPoint = factory.createPoint(
                previewView.width / 2f,
                previewView.height / 2f
            )

            val action = FocusMeteringAction.Builder(
                centerPoint,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            ).build()

            boundCamera?.cameraControl?.startFocusAndMetering(action)

            // Slight zoom improves small QR detection
          //  boundCamera?.cameraControl?.setZoomRatio(1.5f)

        }, ContextCompat.getMainExecutor(this))
    }
    /* ================= TAP TO FOCUS ================= */

    private fun enableTapToFocus() {

        previewView.setOnTouchListener { _, event ->

            if (event.action != MotionEvent.ACTION_DOWN) return@setOnTouchListener true

            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(event.x, event.y)

            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF
            ).build()

            boundCamera?.cameraControl?.startFocusAndMetering(action)

            true
        }
    }

    /* ================= API CALL ================= */

    private fun fetchStudent(qrData: String) {

        // 🔥 SHOW LOADER (MAIN THREAD)
        runOnUiThread {
            scanLoader.visibility = android.view.View.VISIBLE
        }

        lifecycleScope.launch(Dispatchers.IO) {

            try {

                val request = QrRequest(
                    userName = userName,
                    application_number = qrData,
                    attendance_type = attendanceType
                )

                val response = RetrofitClient.api.fetchStudent(request)

                withContext(Dispatchers.Main) {

                    // 🔥 HIDE LOADER
                    scanLoader.visibility = android.view.View.GONE

                    if (response.status == "OK") {

                        val intent = Intent(
                            this@ScannerActivity,
                            VerifyActivity::class.java
                        )

                        intent.putExtra("APPLICATION_NO", response.application_number)
                        intent.putExtra("PROGRAM_ID", response.program_id)
                        intent.putExtra("PROGRAM_NAME", response.program_name)
                        intent.putExtra("STUDENT_NAME", response.student_name)
                        intent.putExtra("DATE_OF_BIRTH", response.dob)
                        intent.putExtra("PHOTO", response.photo_url)
                        intent.putExtra("SIGNATURE", response.signature_url)
                        intent.putExtra("USER_NAME", userName)
                        intent.putExtra("ATTENDANCE_TYPE", attendanceType)

                        cameraProvider?.unbindAll() // 🔥 stop camera here

                        startActivity(intent)

                    }

                    else {
                        showBigScannerMessage(response.message ?: "Invalid QR")

                        // 🔥 RESET FLAG (extra safety)
                        scannedOnce.set(false)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {

                    // 🔥 HIDE LOADER ON ERROR
                    scanLoader.visibility = android.view.View.GONE
                    showBigScannerMessage("Server not reachable")
                }
            }
        }
    }


private fun showBigScannerMessage(message: String) {

    val dialog = androidx.appcompat.app.AlertDialog.Builder(this).create()

    val layout = android.widget.LinearLayout(this)
    layout.orientation = android.widget.LinearLayout.VERTICAL
    layout.setPadding(60, 60, 60, 60)

    val title = android.widget.TextView(this)
    title.text = "WARNING"
    title.textSize = 24f
    title.setPadding(0, 0, 0, 40)
    title.textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER

    val msg = android.widget.TextView(this)
    msg.text = message
    msg.textSize = 20f
    msg.textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
    msg.setPadding(0, 0, 0, 50)

    val button = android.widget.Button(this)
    button.text = "OK"
    button.textSize = 18f

    button.setOnClickListener {
        dialog.dismiss()

        // 🔥 Reset scanner for next scan
        scannedOnce.set(false)

        // 🔥 RESTART CAMERA
        startCamera()
    }

    layout.addView(title)
    layout.addView(msg)
    layout.addView(button)

    dialog.setView(layout)
    dialog.setCancelable(false)
    dialog.show()
}
}