package com.example.qrverificationapp.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.qrverificationapp.R
import com.example.qrverificationapp.data.RetrofitClient
import com.example.qrverificationapp.data.VerifyRequest
import kotlinx.coroutines.*

class VerifyActivity : AppCompatActivity() {

    /* ---------- UI ---------- */
    private lateinit var txtInfo: TextView
    private lateinit var imgPhoto: ImageView
    private lateinit var imgSignature: ImageView
    private lateinit var btnVerify: Button
    private lateinit var btnUnverify: Button
    private lateinit var progressBar: ProgressBar

    /* ---------- DATA ---------- */
    private lateinit var applicationNo: String
    private lateinit var programId: String
    private lateinit var programName: String
    private lateinit var studentName: String
    private lateinit var dob: String
    private lateinit var photoUrl: String
    private lateinit var signatureUrl: String
    private lateinit var userName: String

    private val activityScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        /* ---------- INIT VIEWS ---------- */
        txtInfo = findViewById(R.id.txtQrData)
        imgPhoto = findViewById(R.id.imgPhoto)
        imgSignature = findViewById(R.id.imgSignature)
        btnVerify = findViewById(R.id.btnVerify)
        btnUnverify = findViewById(R.id.btnUnverify)
        progressBar = findViewById(R.id.progressBar)

        /* ---------- GET INTENT DATA ---------- */
        applicationNo = intent.getStringExtra("APPLICATION_NO") ?: ""
        programId     = intent.getStringExtra("PROGRAM_ID") ?: ""
        programName   = intent.getStringExtra("PROGRAM_NAME") ?: "N/A"
        studentName   = intent.getStringExtra("STUDENT_NAME") ?: "N/A"
        dob           = intent.getStringExtra("DATE_OF_BIRTH") ?: "N/A"
        photoUrl     = intent.getStringExtra("PHOTO") ?: ""
        signatureUrl = intent.getStringExtra("SIGNATURE") ?: ""
        userName      = intent.getStringExtra("USER_NAME") ?: ""

        /* ---------- VALIDATION ---------- */
        if (applicationNo.isBlank() || programId.isBlank() || userName.isBlank()) {
            Toast.makeText(this, "Invalid verification data", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        /* ---------- DISPLAY TEXT ---------- */
        txtInfo.text = """
            Application No : $applicationNo
            Program ID     : $programId
            Program Name   : $programName
            Name           : $studentName
            DOB            : $dob
        """.trimIndent()

        /* ---------- LOAD PHOTO (FIXED) ---------- */
        loadImage(
            url = photoUrl,
            imageView = imgPhoto,
            placeholder = R.drawable.ic_user_placeholder
        )

        /* ---------- LOAD SIGNATURE (FIXED) ---------- */
        loadImage(
            url = signatureUrl,
            imageView = imgSignature,
            placeholder = R.drawable.ic_signature_placeholder
        )

        /* ---------- BUTTON ACTIONS ---------- */
        btnVerify.setOnClickListener {
            submitAttendance("VERIFIED", null)
        }

        btnUnverify.setOnClickListener {
            showReasonDialog()
        }
    }

    /* ================= IMAGE LOADER (IMPORTANT) ================= */
    private fun loadImage(
        url: String,
        imageView: ImageView,
        placeholder: Int
    ) {
        if (url.isBlank()) {
            imageView.setImageResource(placeholder)
            return
        }

        Glide.with(this@VerifyActivity)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE) // 🔥 CRITICAL
            .skipMemoryCache(true)                     // 🔥 CRITICAL
            .placeholder(placeholder)
            .error(placeholder)
            .fitCenter()
            .into(imageView)
    }

    /* ================= SUBMIT ATTENDANCE ================= */
    private fun submitAttendance(status: String, reason: String?) {

        progressBar.visibility = View.VISIBLE
        btnVerify.isEnabled = false
        btnUnverify.isEnabled = false

        activityScope.launch {
            try {
                val request = VerifyRequest(
                    application_number = applicationNo,
                    program_id = programId,
                    status = status,
                    reason = reason,
                    userName = userName
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.markAttendance(request)
                }

                progressBar.visibility = View.GONE

                if (response.status == "SUCCESS") {
                    Toast.makeText(
                        this@VerifyActivity,
                        "Attendance marked successfully",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    btnVerify.isEnabled = true
                    btnUnverify.isEnabled = true
                    Toast.makeText(
                        this@VerifyActivity,
                        response.message ?: "Attendance failed",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()

                progressBar.visibility = View.GONE
                btnVerify.isEnabled = true
                btnUnverify.isEnabled = true

                Toast.makeText(
                    this@VerifyActivity,
                    "Server error while marking attendance",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /* ================= UNVERIFY REASON ================= */
    private fun showReasonDialog() {

        val input = EditText(this)
        input.hint = "Enter reason"

        AlertDialog.Builder(this)
            .setTitle("Unverify Attendance")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Reason is required", Toast.LENGTH_SHORT).show()
                } else {
                    submitAttendance("UNVERIFIED", reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}
