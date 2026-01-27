package com.example.qrverificationapp.data

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("qrAttendance/checkLogin.htm")
    suspend fun login(
        @Body request: QrRequest
    ): QrResponse

    @POST("qrAttendance/fetchStudent.htm")
    suspend fun fetchStudent(
        @Body request: QrRequest
    ): QrResponse

    @POST("qrAttendance/markAttendance.htm")
    suspend fun markAttendance(
        @Body request: VerifyRequest
    ): QrResponse
}
