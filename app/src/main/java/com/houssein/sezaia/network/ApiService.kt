package com.houssein.sezaia.network

import com.houssein.sezaia.model.request.AskRepairRequest
import com.houssein.sezaia.model.request.ChangeNumberRequest
import com.houssein.sezaia.model.request.ChangePasswordRequest
import com.houssein.sezaia.model.request.ChangeUsernameRequest
import com.houssein.sezaia.model.request.CreateNewPasswordRequest
import com.houssein.sezaia.model.request.LoginRequest
import com.houssein.sezaia.model.request.SignUpRequest
import com.houssein.sezaia.model.request.ForgotPasswordRequest
import com.houssein.sezaia.model.request.QrCodeRequest
import com.houssein.sezaia.model.request.SaveResponseRequest
import com.houssein.sezaia.model.request.SendEmailRequest
import com.houssein.sezaia.model.response.AskRepairResponse
import com.houssein.sezaia.model.response.ChangeNumberResponse
import com.houssein.sezaia.model.response.ChangePasswordResponse
import com.houssein.sezaia.model.response.ChangeUsernameResponse
import com.houssein.sezaia.model.response.CreateNewPasswordResponse
import com.houssein.sezaia.model.response.ForgotPasswordResponse
import com.houssein.sezaia.model.response.LoginResponse
import com.houssein.sezaia.model.response.Message
import com.houssein.sezaia.model.response.QrCodeResponse
import com.houssein.sezaia.model.response.SaveResponseResponse
import com.houssein.sezaia.model.response.SendEmailResponse
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

    @POST("/register")
    fun signUp(@Body request: SignUpRequest): Call<SignUpResponse>

    @POST("change-password")
    fun createNewPassword(@Body request: CreateNewPasswordRequest): Call<CreateNewPasswordResponse>

    @POST("/exist_qr")
    fun checkQrCode(@Body body: QrCodeRequest): Call<QrCodeResponse>

    @POST("/save_response")
    fun saveResponse(@Body request: SaveResponseRequest): Call<SaveResponseResponse>

    @POST("/send_ask")
    fun sendAsk(@Body askRepairRequest: AskRepairRequest): Call<AskRepairResponse>

    @POST("send_email")
    fun sendEmail(@Body request: SendEmailRequest): Call<SendEmailResponse>

    @POST("/change_username")
    fun changeUsername(@Body request: ChangeUsernameRequest): Call<ChangeUsernameResponse>

    @POST("/change_number")
    fun changeNumber(@Body request: ChangeNumberRequest): Call<ChangeNumberResponse>

    @POST("/change_password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ChangePasswordResponse>



    @POST("/change_email")
    fun changeEmail(@Body request: ChangeNumberRequest): Call<ChangeNumberResponse>








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


