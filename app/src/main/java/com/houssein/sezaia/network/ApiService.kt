package com.houssein.sezaia.network

import com.houssein.sezaia.model.request.AskRepairRequest
import com.houssein.sezaia.model.request.ChangeEmailRequest
import com.houssein.sezaia.model.request.ChangeNumberRequest
import com.houssein.sezaia.model.request.ChangePasswordRequest
import com.houssein.sezaia.model.request.ChangeUsernameRequest
import com.houssein.sezaia.model.request.CreateNewPasswordRequest
import com.houssein.sezaia.model.request.DeleteAccountRequest
import com.houssein.sezaia.model.request.LoginRequest
import com.houssein.sezaia.model.request.QrCodeRequest
import com.houssein.sezaia.model.request.ResendOtpRequest
import com.houssein.sezaia.model.request.SaveResponseRequest
import com.houssein.sezaia.model.request.SendEmailRequest
import com.houssein.sezaia.model.request.SignUpRequest
import com.houssein.sezaia.model.request.VerifyChangeEmailRequest
import com.houssein.sezaia.model.request.VerifyDeleteAccountRequest
import com.houssein.sezaia.model.request.VerifyForgetRequest
import com.houssein.sezaia.model.request.VerifyRegisterRequest
import com.houssein.sezaia.model.response.ApiResponse
import com.houssein.sezaia.model.response.AskRepairResponse
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.model.response.ChangeNumberResponse
import com.houssein.sezaia.model.response.ChangePasswordResponse
import com.houssein.sezaia.model.response.ChangeUsernameResponse
import com.houssein.sezaia.model.response.CreateNewPasswordResponse
import com.houssein.sezaia.model.response.DeleteAccountResponse
import com.houssein.sezaia.model.response.LoginResponse
import com.houssein.sezaia.model.response.Message
import com.houssein.sezaia.model.response.QrCodeResponse
import com.houssein.sezaia.model.response.SaveResponseResponse
import com.houssein.sezaia.model.response.SendEmailResponse
import com.houssein.sezaia.model.response.VerifyDeleteAccountResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("questions")
    fun getQuestions(): Call<List<Message>>

    @POST("/register")
    fun registerUser(@Body registerRequest: SignUpRequest): Call<ApiResponse>

    @POST("/verify_register")
    fun verifyRegister(@Body verifyRequest: VerifyRegisterRequest): Call<ApiResponse>

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

    @POST("/delete_account")
    fun deleteAccount(@Body request: DeleteAccountRequest): Call<DeleteAccountResponse>

    @POST("/forgot_password")
    fun forgotPassword(@Body request: ResendOtpRequest): Call<BaseResponse>

    @POST("/resend_otp")
    fun resendOtp(@Body request: ResendOtpRequest): Call<BaseResponse>

    @POST("/verify_forget")
    fun verifyForget(@Body request: VerifyForgetRequest): Call<BaseResponse>

    @POST("/change_email")
    fun changeEmail(@Body request: ChangeEmailRequest): Call<BaseResponse>

    @POST("/verify_change_email")
    fun verifyChangeEmail(@Body request: VerifyChangeEmailRequest): Call<BaseResponse>


    @POST("/verify_delete_account")
    fun verifyDeleteAccount(@Body request: VerifyDeleteAccountRequest): Call<VerifyDeleteAccountResponse>
}

