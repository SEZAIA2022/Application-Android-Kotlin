package com.houssein.sezaia.ui.screen

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.request.NotificationRequest
import com.houssein.sezaia.model.request.SendAskDirectRequest
import com.houssein.sezaia.model.request.TechnicianRequest
import com.houssein.sezaia.model.response.QrIdResponse
import com.houssein.sezaia.model.response.SendAskDirectResponse
import com.houssein.sezaia.model.response.TechnicianResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.adapter.QrIdAdapter
import com.houssein.sezaia.ui.utils.UIUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RequestInterventionDirectActivity : BaseActivity() {

    private lateinit var recycler: RecyclerView

    private lateinit var loggedEmail: String
    private lateinit var loggedUsername: String
    private lateinit var applicationName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_request_intervention_direct)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))

        val app = applicationContext as MyApp
        applicationName = app.application_name
        val type = app.application_type

        // Toolbar
        UIUtils.initToolbar(
            this,
            getString(R.string.request_intervention),
            actionIconRes = R.drawable.baseline_density_medium_24,
            onBackClick = { if (type == "direct") {} else finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        val spLogin = getSharedPreferences("LoginData", MODE_PRIVATE)
        loggedEmail = spLogin.getString("LoggedEmail", "") ?: ""
        loggedUsername = spLogin.getString("loggedUsername", "") ?: ""

        recycler = findViewById(R.id.qrRecycler)
        recycler.layoutManager = LinearLayoutManager(this)

        // charge la liste dès l’ouverture
        loadQrIds()
    }

    private fun loadQrIds() {
        RetrofitClient.instance.getIdQrs(applicationName, loggedUsername)
            .enqueue(object : Callback<QrIdResponse> {
                override fun onResponse(call: Call<QrIdResponse>, resp: Response<QrIdResponse>) {
                    if (!resp.isSuccessful || resp.body()?.status != "success") {
                        val msg = resp.body()?.message ?: getString(R.string.loading_error)
                        Toast.makeText(this@RequestInterventionDirectActivity, msg, Toast.LENGTH_LONG).show()
                        return
                    }
                    val list = resp.body()?.qr_ids ?: emptyList()
                    recycler.adapter = QrIdAdapter(list) { qrId ->
                        openRequestDialog(qrId)
                    }
                }

                @SuppressLint("StringFormatInvalid")
                override fun onFailure(call: Call<QrIdResponse>, t: Throwable) {
                    Toast.makeText(this@RequestInterventionDirectActivity,
                        getString(R.string.network_error, t.localizedMessage),
                        Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun openRequestDialog(qrId: String) {
        val view = layoutInflater.inflate(R.layout.dialog_request_intervention, null)
        val commentInput = view.findViewById<TextInputEditText>(R.id.commentInput)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.request_intervention))
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.send) { _, _ ->
                val commentText = commentInput.text?.toString()?.trim().orEmpty()
                if (commentText.isBlank()) {
                    Toast.makeText(this, R.string.all_fields_required, Toast.LENGTH_SHORT).show()
                } else {
                    createDirectAsk(qrId, commentText)
                }
            }
            .create()

        dialog.setOnShowListener {
            val blueColor = ContextCompat.getColor(this, R.color.blue)
            // 🔹 R.color.blue_500 doit exister dans res/values/colors.xml

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(blueColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(blueColor)
        }

        dialog.show()
    }


    private fun createDirectAsk(qrId: String, commentText: String) {
        // 1) récupérer technicien
        val reqTech = TechnicianRequest(
            email = loggedEmail,
            application_name = "",
            date = "",
            hour_slot = ""
        )

        RetrofitClient.instance.getNearestAdminEmail(reqTech)
            .enqueue(object : Callback<TechnicianResponse> {
                override fun onResponse(call: Call<TechnicianResponse>, response: Response<TechnicianResponse>) {
                    val techEmail = response.body()?.email
                    if (!response.isSuccessful || response.body()?.status != "success" || techEmail.isNullOrBlank()) {
                        Toast.makeText(this@RequestInterventionDirectActivity,
                            response.body()?.message ?: getString(R.string.no_tech_available),
                            Toast.LENGTH_LONG).show()
                        return
                    }

                    // 2) envoyer la demande
                    val createReq = SendAskDirectRequest(
                        username = loggedUsername.ifBlank { loggedEmail },
                        comment = commentText,
                        qr_id = qrId,
                        application_name = applicationName,
                        technician_email = techEmail
                    )

                    RetrofitClient.instance.sendAskDirect(createReq)
                        .enqueue(object : Callback<SendAskDirectResponse> {
                            override fun onResponse(
                                call: Call<SendAskDirectResponse>,
                                resp: Response<SendAskDirectResponse>
                            ) {
                                if (resp.isSuccessful) {
                                    val body = resp.body()

                                    if (body?.status.equals("success", ignoreCase = true)) {
                                        Toast.makeText(
                                            this@RequestInterventionDirectActivity,
                                            body?.message ?: getString(R.string.request_sent),
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        sendNotification(
                                            "admin",
                                            techEmail,
                                            "Direct intervention request\nQR: $qrId\nComment: $commentText"
                                        ) {
                                            sendNotification(
                                                "user",
                                                loggedEmail,
                                                "Your direct intervention request has been sent."
                                            ) { }
                                        }

                                    } else {
                                        // 200 OK mais status = error
                                        Toast.makeText(
                                            this@RequestInterventionDirectActivity,
                                            body?.message ?: getString(R.string.booking_error),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                } else {

                                    val errorMessage = UIUtils.extractErrorMessage(resp)

                                    Toast.makeText(
                                        this@RequestInterventionDirectActivity,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }


                            @SuppressLint("StringFormatInvalid")
                            override fun onFailure(call: Call<SendAskDirectResponse>, t: Throwable) {
                                Toast.makeText(this@RequestInterventionDirectActivity,
                                    getString(R.string.network_error, t.localizedMessage),
                                    Toast.LENGTH_LONG).show()
                            }
                        })
                }

                @SuppressLint("StringFormatInvalid")
                override fun onFailure(call: Call<TechnicianResponse>, t: Throwable) {
                    Toast.makeText(this@RequestInterventionDirectActivity,
                        getString(R.string.network_error, t.localizedMessage),
                        Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun sendNotification(role: String, email: String, message: String, onDone: () -> Unit) {
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest(
                message = message,
                role = role,
                email = email,
                application_name = applicationName
            )
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onDone()
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                onDone()
            }
        })
    }
}
