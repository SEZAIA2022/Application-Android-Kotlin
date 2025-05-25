package com.houssein.sezaia.network

import com.houssein.sezaia.model.request.LoginRequest
import com.houssein.sezaia.model.request.SignUpRequest
import com.houssein.sezaia.model.response.ForgotPasswordRequest
import com.houssein.sezaia.model.response.ForgotPasswordResponse
import com.houssein.sezaia.model.response.LoginResponse
import com.houssein.sezaia.model.response.Message
import com.houssein.sezaia.model.response.SignUpResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("questions")
    fun getQuestions(): Call<List<Message>>

    @POST("/forgot_password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ForgotPasswordResponse>

    @POST("/sign_up")
    fun signUp(@Body request: SignUpRequest): Call<SignUpResponse>


    @POST("/verify_forget")
    fun verifyForgetOtp(@Body body: Map<String, String>): Call<Map<String, Any>>

    @POST("/verify_register")
    fun verifyRegisterOtp(@Body body: Map<String, String>): Call<Map<String, Any>>

    @POST("/verify_change_email")
    fun verifyChangeEmailOtp(@Body body: Map<String, String>): Call<Map<String, Any>>

    @POST("/delete_account")
    fun deleteAccount(@Body body: Map<String, String>): Call<Map<String, Any>>

    @POST("/resend_otp")
    fun resendOtp(@Body body: Map<String, String>): Call<Map<String, Any>>


}


