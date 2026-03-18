package com.example.qrverificationapp.data

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy


object RetrofitClient {

    // ✅ MUST include context path + trailing slash
    private const val BASE_URL =
      // "http://10.20.12.99:8080/Admission_Panel/"
     //   "http://10.133.94.68:8080/Admission_Panel/"
   // "http://10.219.39.68:8080/Admission_Panel/"
      //  "http://192.168.29.244:8080/Admission_Panel/"
        "https://admission.dei.ac.in/admission_panel_server/"


    // ✅ Logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ✅ Custom Gson for Spring MVC compatibility
    private val gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    // ✅ Cookie manager (stores JSESSIONID)
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }
    val okHttpClient = OkHttpClient.Builder()
        .cookieJar(JavaNetCookieJar(cookieManager)) // ⭐ session sharing
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
