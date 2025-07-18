package com.houssein.sezaia.network

import RepairResponse
import com.houssein.sezaia.model.request.*
import com.houssein.sezaia.model.response.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/register")
    fun registerUser(@Body registerRequest: SignUpRequest): Call<ApiResponse>

    @POST("api/verify_register")
    fun verifyRegister(@Body verifyRequest: VerifyRegisterRequest): Call<ApiResponse>

    @POST("api/forgot_password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<BaseResponse>

    @POST("api/resend_otp")
    fun resendOtp(@Body request: ResendOtpRequest): Call<BaseResponse>

    @POST("api/verify_forget")
    fun verifyForget(@Body request: VerifyForgetRequest): Call<BaseResponse>

    @POST("api/change-password")
    fun createNewPassword(@Body request: CreateNewPasswordRequest): Call<CreateNewPasswordResponse>

    @POST("api/exist_qr")
    fun checkQrCode(@Body body: QrCodeRequest): Call<QrCodeResponse>

    @POST("api/add_qr")
    fun addQr(@Body request: AddQrRequest): Call<BaseResponse>

    @GET("api/questions")
    fun getQuestions(@Query("application") application: String): Call<List<Message>>

    @POST("api/send_ask_and_response")
    fun sendAskRepairWithResponses(@Body request: AskRepairWithResponsesRequest): Call<BaseResponse>

    @GET("api/taken_slots")
    fun getTakenSlots(): Call<TakenSlotsResponse>

    @POST("api/send_email")
    fun sendEmail(@Body request: SendEmailRequest): Call<SendEmailResponse>

    @GET("api/ask_repair")
    suspend fun getRepairs(
        @Query("username") username: String,
        @Query("application") application: String = "sezaia"
    ): List<Repair>

    @POST("api/cancel_appointment")
    suspend fun cancelAppointment(@Body request: CancelAppointmentRequest): CancelAppointmentResponse

    @GET("api/about_us")
    fun getAboutUs(@Query("application") application: String): Call<AboutUsResponse>

    @GET("api/term_of_use")
    fun getTermsOfUse(@Query("application") application: String): Call<TermsOfUseResponse>

    @GET("api/privacy_policy")
    fun getPrivacyPolicy(@Query("application") application: String): Call<PrivacyPolicyResponse>







    @POST("api/change_username")
    fun changeUsername(@Body request: ChangeUsernameRequest): Call<ChangeUsernameResponse>

    @POST("api/change_number")
    fun changeNumber(@Body request: ChangeNumberRequest): Call<ChangeNumberResponse>

    @POST("api/change_password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ChangePasswordResponse>

    @POST("api/delete_account")
    fun deleteAccount(@Body request: DeleteAccountRequest): Call<BaseResponse>


    @POST("api/change_email")
    fun changeEmail(@Body request: ChangeEmailRequest): Call<BaseResponse>

    @POST("api/verify_change_email")
    fun verifyChangeEmail(@Body request: VerifyChangeEmailRequest): Call<BaseResponse>

    @POST("api/verify_delete_account")
    fun verifyDeleteAccount(@Body request: VerifyDeleteAccountRequest): Call<BaseResponse>

    @GET("api/get_qrcodes")
    fun getQRCodes(): Call<QrCodeResponse>


    @GET("api/ask_repair/details/{repair_id}")
    suspend fun getRepairDetails(
        @Path("repair_id") repairId: String?
    ): RepairResponse

    @POST("api/add_description")
    suspend fun addDescription(@Body request: DescriptionRequest): BaseResponse

    @GET("api/get_repair_by_qrcode_full")
    fun fetchRepairByQrCode(
        @Query("qr_code") qrCode: String
    ): Call<RepairApiResponse>

    @GET("api/help_tasks")
    fun getHelpTasks(): Call<HelpResponse>


}
