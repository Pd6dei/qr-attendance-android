package com.example.qrverificationapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.qrverificationapp.R
import com.example.qrverificationapp.data.SessionManager

class AttendanceTypeActivity : AppCompatActivity() {

    private lateinit var userName: String
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_type)

        sessionManager = SessionManager(this)

        // get username (intent OR session)
        userName = intent.getStringExtra("USER_NAME")
            ?: sessionManager.getUser().orEmpty()

        if (userName.isNotEmpty()) {
            sessionManager.saveUser(userName)
        }

        findViewById<LinearLayout>(R.id.btnET)
            .setOnClickListener { openScanner("ET") }

        findViewById<LinearLayout>(R.id.btnPI)
            .setOnClickListener { openScanner("PW") }

        findViewById<LinearLayout>(R.id.btnGD)
            .setOnClickListener { openScanner("GD") }

        findViewById<LinearLayout>(R.id.btnFS)
            .setOnClickListener { openScanner("FS") }

        // Excel download
        findViewById<ImageView>(R.id.btnExcel)
            .setOnClickListener {
                generateExcelReport()
            }
    }

    private fun openScanner(typeCode: String) {

        sessionManager.saveAttendanceType(typeCode)

        val intent = Intent(this, ScannerActivity::class.java)
        intent.putExtra("ATTENDANCE_TYPE", typeCode)
        intent.putExtra("USER_NAME", userName)
        startActivity(intent)
        finish()
    }

    private fun generateExcelReport() {

        val user = sessionManager.getUser()

        if (user.isNullOrEmpty()) return

        val intent = Intent(this, ExcelDownloadActivity::class.java)
        intent.putExtra("USER_NAME", user)
        startActivity(intent)
    }
}
