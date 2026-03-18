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
import com.example.qrverificationapp.data.SessionManager
import com.example.qrverificationapp.data.VerifyRequest
import kotlinx.coroutines.*

class VerifyActivity : AppCompatActivity() {

    private lateinit var imgPhoto: ImageView
    private lateinit var imgSignature: ImageView
    private lateinit var btnVerify: Button
    private lateinit var btnUnverify: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var txtHeader: TextView

    private lateinit var applicationNo: String
    private lateinit var programId: String
    private lateinit var programName: String
    private lateinit var studentName: String
    private lateinit var dob: String
    private lateinit var photoUrl: String
    private lateinit var signatureUrl: String
    private lateinit var userName: String
    private lateinit var attendanceType: String

    private lateinit var sessionManager: SessionManager

    private val activityScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        sessionManager = SessionManager(this)

        imgPhoto = findViewById(R.id.imgPhoto)
        imgSignature = findViewById(R.id.imgSignature)
        btnVerify = findViewById(R.id.btnVerify)
        btnUnverify = findViewById(R.id.btnUnverify)
        progressBar = findViewById(R.id.progressBar)
        txtHeader = findViewById(R.id.txtHeader)

        applicationNo = intent.getStringExtra("APPLICATION_NO") ?: ""
        programId     = intent.getStringExtra("PROGRAM_ID") ?: ""
        programName   = intent.getStringExtra("PROGRAM_NAME") ?: "N/A"
        studentName   = intent.getStringExtra("STUDENT_NAME") ?: "N/A"
        dob           = intent.getStringExtra("DATE_OF_BIRTH") ?: "N/A"
        photoUrl      = intent.getStringExtra("PHOTO") ?: ""
        signatureUrl  = intent.getStringExtra("SIGNATURE") ?: ""

        // intent OR session fallback
        userName = intent.getStringExtra("USER_NAME")
            ?: sessionManager.getUser().orEmpty()

        attendanceType = intent.getStringExtra("ATTENDANCE_TYPE")
            ?: sessionManager.getAttendanceType().orEmpty()

        txtHeader.text = when(attendanceType){
            "ET" -> "Entrance Test Attendance"
            "PW" -> "Personal Interview Attendance"
            "GD" -> "Group Discussion Attendance"
            "FS" -> "Final Selection Attendance"
            else -> "Attendance Verification"
        }

        if (applicationNo.isBlank() || programId.isBlank() || userName.isBlank()  ||
            attendanceType.isBlank()) {
            Toast.makeText(this, "Invalid verification data", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bindRow(R.id.rowApplicationNo, "Application No", applicationNo)
        bindRow(R.id.rowProgramName, "Program Name", programName)
        bindRow(R.id.rowStudentName, "Name", studentName)
        bindRow(R.id.rowDob, "DOB", dob)

        loadImage(photoUrl, imgPhoto, R.drawable.ic_user_placeholder)
        loadImage(signatureUrl, imgSignature, R.drawable.ic_signature_placeholder)

      //  btnVerify.setOnClickListener {
       //     if (progressBar.visibility == View.VISIBLE) return@setOnClickListener
        //    checkAttendanceBeforeVerify()
      //  }

      //  btnUnverify.setOnClickListener {
        //    if (progressBar.visibility == View.VISIBLE) return@setOnClickListener
        //    checkAttendanceBeforeReject()
      //  }

        btnVerify.setOnClickListener {
           if (progressBar.visibility == View.VISIBLE) return@setOnClickListener
            submitAttendance("VERIFIED", null)
        }




        btnUnverify.setOnClickListener {
            if (progressBar.visibility == View.VISIBLE) return@setOnClickListener
          showReasonDialog()
        }
    }



    private fun bindRow(rowId: Int, label: String, value: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.txtLabel).text = label
        row.findViewById<TextView>(R.id.txtValue).text = value
    }

    private fun loadImage(url: String, imageView: ImageView, placeholder: Int) {
        if (url.isBlank()) {
            imageView.setImageResource(placeholder)
            return
        }

        Glide.with(this)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(placeholder)
            .error(placeholder)
            .fitCenter()
            .dontTransform()
            .into(imageView)
    }

    private fun submitAttendance(status: String, reason: String?) {

        progressBar.visibility = View.VISIBLE
        btnVerify.isEnabled = false
        btnUnverify.isEnabled = false

        activityScope.launch {
            try {

                val request = VerifyRequest(
                    application_number = applicationNo,
                    program_id = programId,
                    attendance_type = attendanceType,
                    status = status,
                    reason = reason,
                    userName = userName
                )

                val response = withContext(Dispatchers.IO) {
                   // RetrofitClient.api.markAttendance(request)
                    if (status == "VERIFIED") {
                        RetrofitClient.api.verifyAttendance(request)
                    } else {
                        RetrofitClient.api.unverifyAttendance(request)
                    }
                }

                progressBar.visibility = View.GONE
                btnVerify.isEnabled = true
                btnUnverify.isEnabled = true

             //   if (response.status == "SUCCESS") {
              //      Toast.makeText(
               //         this@VerifyActivity,
               //         if (status == "VERIFIED")
               //             "Attendance Verified Successfully"
               //         else
               //             "Attendance Rejected Successfully",
               //         Toast.LENGTH_LONG
               //     ).show()
               //     finish()
              //  } else {
                 //   Toast.makeText(
                 //       this@VerifyActivity,
                 //       response.message ?: "Attendance already marked",
                 //       Toast.LENGTH_LONG
                 //   ).show()
              //
            //
            //  }
                when (response.status) {

                    "SUCCESS" -> {

                        Toast.makeText(
                            this@VerifyActivity,
                            if (status == "VERIFIED")
                                "Attendance Verified Successfully"
                            else
                                "Attendance Rejected Successfully",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }

                    "DUPLICATE" -> {

                        AlertDialog.Builder(this@VerifyActivity)
                            .setTitle("Attendance Status")
                            .setMessage("Attendance Already Marked")
                            .setPositiveButton("OK") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    }

                    "FAILED", "ERROR" -> {

                        Toast.makeText(
                            this@VerifyActivity,
                            response.message ?: "Attendance could not be saved",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> {

                        Toast.makeText(
                            this@VerifyActivity,
                            "Unexpected server response",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {

                e.printStackTrace()
                println("VERIFY ERROR: " + e.message)

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

    private fun showReasonDialog() {

        val input = EditText(this)
        input.hint = "Enter reason"

        AlertDialog.Builder(this)
            .setTitle("Rejection Reason")
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



    private fun showBigAlreadyMarkedDialog() {

        val messageText = TextView(this)
        messageText.text = "ATTENDANCE IS ALREADY MARKED"
        messageText.textSize = 15f
        messageText.setPadding(40, 40, 40, 40)
        messageText.textAlignment = View.TEXT_ALIGNMENT_CENTER

        AlertDialog.Builder(this)
            .setTitle("Attendance Status")
            .setView(messageText)
            .setPositiveButton("Ok") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}
