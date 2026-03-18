package com.example.qrverificationapp.data

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

interface ApiService {

    @POST("qrAttendance/checkLogin.htm")
    suspend fun login(
        @Body request: QrRequest
    ): QrResponse

    @POST("qrAttendance/fetchStudent.htm")
    suspend fun fetchStudent(
        @Body request: QrRequest
    ): QrResponse

   // @POST("qrAttendance/markAttendance.htm")
   // suspend fun markAttendance(
       // @Body request: VerifyRequest
   // ): QrResponse

    @POST("qrAttendance/verifyAttendance.htm")
    suspend fun verifyAttendance(
        @Body request: VerifyRequest
    ): VerifyResponse

    @POST("qrAttendance/unverifyAttendance.htm")
    suspend fun unverifyAttendance(
        @Body request: VerifyRequest
    ): VerifyResponse

}
