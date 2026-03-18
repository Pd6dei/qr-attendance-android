package com.example.qrverificationapp.ui

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy
import com.example.qrverificationapp.data.RetrofitClient

class ExcelDownloadActivity : AppCompatActivity() {

    private val STORAGE_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            } else {
                startDownload()
            }
        } else {
            startDownload()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload()
            } else {
                Toast.makeText(this, "Storage Permission Required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startDownload() {

        val userName = intent.getStringExtra("USER_NAME")

        if (userName.isNullOrEmpty()) {
            Toast.makeText(this, "UserName missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        downloadExcel(userName)
    }

    private fun downloadExcel(userName: String) {

        //val client = OkHttpClient()

        val client = RetrofitClient.okHttpClient

        val url =
           // "http://10.20.12.99:8080/Admission_Panel/qrAttendance/generateExcel.htm?userName=$userName"
            "https://admission.dei.ac.in/admission_panel_server/qrAttendance/generateExcel.htm?userName=$userName"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@ExcelDownloadActivity,
                        "Download Failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ExcelDownloadActivity,
                            "Server Error: ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return
                }

                try {

                    val fileName = "AttendanceReport.xlsx"
                    val mimeType =
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+

                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, mimeType)
                            put(
                                MediaStore.Downloads.RELATIVE_PATH,
                                Environment.DIRECTORY_DOWNLOADS
                            )
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }

                        val uri = contentResolver.insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            values
                        )!!

                        contentResolver.openOutputStream(uri)?.use { output ->
                            response.body!!.byteStream().copyTo(output)
                        }

                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)

                        runOnUiThread {
                            Toast.makeText(
                                this@ExcelDownloadActivity,
                                "Saved in Downloads",
                                Toast.LENGTH_LONG
                            ).show()
                            openExcelFile(uri)
                        }

                    } else {
                        // Android 9

                        val downloadsDir =
                            Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            )

                        if (!downloadsDir.exists()) downloadsDir.mkdirs()

                        val file = File(downloadsDir, fileName)

                        FileOutputStream(file).use { output ->
                            response.body!!.byteStream().copyTo(output)
                        }

                        val uri = Uri.fromFile(file)

                        runOnUiThread {
                            Toast.makeText(
                                this@ExcelDownloadActivity,
                                "Saved: ${file.absolutePath}",
                                Toast.LENGTH_LONG
                            ).show()
                            openExcelFile(uri)
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ExcelDownloadActivity,
                            "File Save Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        })
    }

    private fun openExcelFile(uri: Uri) {

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No Excel app found",
                Toast.LENGTH_LONG
            ).show()
        }

        finish()
    }
}