package com.houssein.sezaia.network

import com.houssein.sezaia.model.request.*
import com.houssein.sezaia.model.response.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/questions")
    fun getQuestions(): Call<List<Message>>

    @POST("api/register")
    fun registerUser(@Body registerRequest: SignUpRequest): Call<ApiResponse>

    @POST("api/verify_register")
    fun verifyRegister(@Body verifyRequest: VerifyRegisterRequest): Call<ApiResponse>

    @POST("api/change-password")
    fun createNewPassword(@Body request: CreateNewPasswordRequest): Call<CreateNewPasswordResponse>

    @POST("api/exist_qr")
    fun checkQrCode(@Body body: QrCodeRequest): Call<QrCodeResponse>

    @POST("api/save_response")
    fun saveResponse(@Body request: SaveResponseRequest): Call<SaveResponseResponse>

    @POST("api/send_ask")
    fun sendAsk(@Body askRepairRequest: AskRepairRequest): Call<AskRepairResponse>

    @POST("api/send_email")
    fun sendEmail(@Body request: SendEmailRequest): Call<SendEmailResponse>

    @POST("api/change_username")
    fun changeUsername(@Body request: ChangeUsernameRequest): Call<ChangeUsernameResponse>

    @POST("api/change_number")
    fun changeNumber(@Body request: ChangeNumberRequest): Call<ChangeNumberResponse>

    @POST("api/change_password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ChangePasswordResponse>

    @POST("api/delete_account")
    fun deleteAccount(@Body request: DeleteAccountRequest): Call<BaseResponse>

    @POST("api/forgot_password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<BaseResponse>

    @POST("api/resend_otp")
    fun resendOtp(@Body request: ResendOtpRequest): Call<BaseResponse>

    @POST("api/verify_forget")
    fun verifyForget(@Body request: VerifyForgetRequest): Call<BaseResponse>

    @POST("api/change_email")
    fun changeEmail(@Body request: ChangeEmailRequest): Call<BaseResponse>

    @POST("api/verify_change_email")
    fun verifyChangeEmail(@Body request: VerifyChangeEmailRequest): Call<BaseResponse>

    @POST("api/verify_delete_account")
    fun verifyDeleteAccount(@Body request: VerifyDeleteAccountRequest): Call<BaseResponse>

    @POST("api/add_qr")
    fun addQr(@Body request: AddQrRequest): Call<BaseResponse>

    @GET("api/ask_repair")
    suspend fun getRepairs(@Query("username") username: String): List<Repair>

    @GET("api/taken_slots")
    fun getTakenSlots(): Call<TakenSlotsResponse>

    @GET("api/about_us")
    fun getAboutUs(): Call<AboutUsResponse>

    @GET("api/term_of_use")
    fun getTermsOfUse(): Call<TermsOfUseResponse>

    @GET("api/privacy_policy")
    fun getPrivacyPolicy(): Call<PrivacyPolicyResponse>

    @POST("api/cancel_appointment")
    suspend fun cancelAppointment(@Body request: CancelAppointmentRequest): CancelAppointmentResponse

    @GET("api/help_tasks") // Assure-toi que ce chemin correspond bien à l'URL Flask
    fun getHelpTasks(): Call<HelpTasksResponse>


}
