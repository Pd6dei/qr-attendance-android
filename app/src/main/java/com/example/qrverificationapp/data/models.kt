package com.example.qrverificationapp.data

// ========================
// LOGIN + FETCH STUDENT BY QR
// ========================
data class QrRequest(
    val userName: String? = null,
    val password: String? = null,
    val application_number: String? = null
)

// ========================
// MARK ATTENDANCE (FIXED)
// ========================
data class VerifyRequest(
    val application_number: String,
    val program_id: String,
    val status: String,
    val reason: String? = null,
    val userName: String        // ✅ REQUIRED by backend
)

// ========================
// RESPONSE (FROM SPRING)
// ========================
data class QrResponse(
    val status: String,                 // e.g. SUCCESS / FAIL
    val message: String? = null,
    val application_number: String? = null,
    val program_id: String? = null,
    val program_name: String? = null,
    val student_name: String? = null,   // from DB
    val dob: String? = null,
    val photo_url: String?,        // ✅ URL
    val signature_url: String?,    // ✅ URL
)
