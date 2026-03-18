package com.example.qrverificationapp.data

// ========================
// LOGIN + FETCH STUDENT BY QR
// ========================
data class QrRequest(
    val userName: String? = null,
    val password: String? = null,
    val application_number: String? = null,
    val attendance_type: String? = null
)

// ========================
// MARK ATTENDANCE (REQUEST)
// ========================
data class VerifyRequest(
    val application_number: String,
    val program_id: String,
    val attendance_type: String,
    val status: String,
    val reason: String? = null,
    val userName: String
)

// ========================
// MARK ATTENDANCE RESPONSE
// ========================
data class VerifyResponse(
    val status: String,
    val attendance_state: String?,
    val message: String?
)

// ========================
// FETCH STUDENT RESPONSE
// ========================
data class QrResponse(
    val status: String? = null,
    val message: String? = null,
    val application_number: String? = null,
    val program_id: String? = null,
    val program_name: String? = null,
    val student_name: String? = null,
    val dob: String? = null,
    val photo_url: String? = null,
    val signature_url: String? = null
)
