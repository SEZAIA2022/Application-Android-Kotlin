package com.houssein.sezaia.ui.screen

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.houssein.sezaia.R
import com.houssein.sezaia.model.data.DayItem
import com.houssein.sezaia.model.data.MyApp
import com.houssein.sezaia.model.data.QuestionAnswer
import com.houssein.sezaia.model.request.AskRepairWithResponsesRequest
import com.houssein.sezaia.model.request.NotificationRequest
import com.houssein.sezaia.model.request.ResponseItem
import com.houssein.sezaia.model.request.TechnicianRequest
import com.houssein.sezaia.model.response.BaseResponse
import com.houssein.sezaia.model.response.TakenSlotsResponse
import com.houssein.sezaia.model.response.TechnicianResponse
import com.houssein.sezaia.network.RetrofitClient
import com.houssein.sezaia.ui.BaseActivity
import com.houssein.sezaia.ui.utils.UIUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class AppointmentActivity : BaseActivity() {

    private lateinit var scroll: NestedScrollView
    private lateinit var daysRecyclerView: RecyclerView
    private lateinit var confirmButton: MaterialButton
    private lateinit var commentEditText: TextInputEditText
    private lateinit var commentLayout: TextInputLayout

    private lateinit var applicationName: String
    private lateinit var loggedUser: String
    private lateinit var loggedEmail: String
    private lateinit var qrData: String

    private var totalTechs = 0
    private var selectedDate: LocalDate? = null
    private var selectedTimeSlot: String? = null
    private var commentText: String = ""

    private lateinit var responseList: List<QuestionAnswer>
    private var takenSlotsMap: Map<String, Map<String, Int>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redimensionner la zone utile quand le clavier apparaît
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContentView(R.layout.activity_appointment)
        UIUtils.applySystemBarsInsets(findViewById(R.id.main))
        UIUtils.initToolbar(
            this, getString(R.string.appointment), R.drawable.baseline_density_medium_24,
            onBackClick = { finish() },
            onActionClick = { startActivity(Intent(this, SettingsActivity::class.java)) }
        )

        // Views
        scroll = findViewById(R.id.scroll)
        applyImeAndSystemBarsPadding(scroll)
        hookImeAnimationForAutoScroll(scroll)

        daysRecyclerView = findViewById(R.id.daysRecyclerView)
        confirmButton = findViewById(R.id.confirmButton)
        commentEditText = findViewById(R.id.comment)
        commentLayout = findViewById(R.id.commentLayout)
        confirmButton.isEnabled = false

        // Prefs
        val spLogin = getSharedPreferences("LoginData", MODE_PRIVATE)
        loggedUser = spLogin.getString("loggedUsername", "") ?: ""
        loggedEmail = spLogin.getString("LoggedEmail", "") ?: ""
        qrData = getSharedPreferences("MyPrefs", MODE_PRIVATE).getString("qrData", "") ?: ""
        applicationName = (application as MyApp).application_name

        @Suppress("UNCHECKED_CAST")
        responseList =
            (intent.getSerializableExtra("responses") as? ArrayList<*>)?.filterIsInstance<QuestionAnswer>()
                ?: emptyList()

        // Validation du commentaire + maj bouton
        commentEditText.addTextChangedListener(UIUtils.inputWatcher(commentEditText, commentLayout))
        commentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                commentText = s?.toString().orEmpty()
                updateConfirmButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fetchTakenSlots(loggedUser, applicationName) {
            if (totalTechs <= 0) {
                Toast.makeText(this, R.string.no_tech_available, Toast.LENGTH_LONG).show()
                confirmButton.isEnabled = false
            } else {
                setupDaysRecyclerView()
            }
        }

        confirmButton.setOnClickListener { onConfirmClick() }

        // Auto-scroll sur focus du commentaire (au cas où l’utilisateur clique dedans manuellement)
        commentEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) v.postDelayed({ scrollToViewOnScreen(v) }, 120)
        }
    }

    // === Clavier & scroll ===

    /** Ajoute padding bas = max(barres système, clavier) pour garder les éléments visibles */
    private fun applyImeAndSystemBarsPadding(target: View) {
        val l = target.paddingLeft
        val t = target.paddingTop
        val r = target.paddingRight
        val baseB = target.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(left = l, top = t, right = r, bottom = baseB + maxOf(sys.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
    }

    /** Pendant l’animation d’ouverture du clavier, on recentre la vue focus à la fin. */
    private fun hookImeAnimationForAutoScroll(target: View) {
        ViewCompat.setWindowInsetsAnimationCallback(
            target,
            object : WindowInsetsAnimationCompat.Callback(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            ) {
                // Implémentation requise par AndroidX
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        currentFocus?.post { currentFocus?.let { scrollToViewOnScreen(it) } }
                    }
                }
            }
        )
    }

    /** Fait défiler jusqu’à ce que la vue soit dans le viewport du NestedScrollView. */
    private fun scrollToViewOnScreen(target: View) {
        if (target.width == 0 || target.height == 0) return
        val r = Rect(0, 0, target.width, target.height)
        target.requestRectangleOnScreen(r, true)
    }

    private fun dp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    // === UI ===

    private fun focusCommentAndShowIme() {
        commentEditText.requestFocus()
        commentEditText.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(commentEditText, InputMethodManager.SHOW_IMPLICIT)
            scrollToViewOnScreen(commentEditText)
        }
    }

    private fun setupDaysRecyclerView() {
        val days = generateUpcomingDays(14)
        val adapter = DaysAdapter(days) { dayItem, timeSlot ->
            // Sélection du créneau
            selectedDate = dayItem.localDate
            selectedTimeSlot = timeSlot
            updateConfirmButtonState()

            // Mettre directement le focus sur le commentaire + clavier + scroll
            focusCommentAndShowIme()
        }
        daysRecyclerView.layoutManager = LinearLayoutManager(this)
        daysRecyclerView.adapter = adapter
        daysRecyclerView.isNestedScrollingEnabled = false
    }

    private fun updateConfirmButtonState() {
        val enabled = selectedDate != null && !selectedTimeSlot.isNullOrEmpty() && commentText.isNotBlank()
        confirmButton.isEnabled = enabled
        confirmButton.alpha = if (enabled) 1f else 0.5f
    }

    private fun onConfirmClick() {
        val date = selectedDate ?: return Toast.makeText(this, R.string.fill_required_fields, Toast.LENGTH_SHORT).show()
        val slot = selectedTimeSlot ?: return Toast.makeText(this, R.string.fill_required_fields, Toast.LENGTH_SHORT).show()
        if (commentText.isBlank()) return Toast.makeText(this, R.string.fill_required_fields, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val dateKey = date.format(DateTimeFormatter.ISO_DATE)
            val technicianEmail = fetchNearestTechnicianEmailSuspend(loggedEmail, applicationName, dateKey, slot)
            if (technicianEmail == null) {
                Toast.makeText(this@AppointmentActivity, R.string.no_tech_this_slot, Toast.LENGTH_LONG).show()
                return@launch
            }

            val outFmt = DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.ENGLISH)
            val formattedDate = "${date.format(outFmt)} $slot"
            val responsesPayload = responseList.map { ResponseItem(it.id, it.answer) }

            val req = AskRepairWithResponsesRequest(
                loggedUser, formattedDate, commentText, qrData, responsesPayload, applicationName, technicianEmail
            )

            RetrofitClient.instance.sendAskRepairWithResponses(req)
                .enqueue(object : Callback<BaseResponse> {
                    override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            Toast.makeText(this@AppointmentActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                            sendNotificationToAdmin(technicianEmail, applicationName)
                            sendNotificationToUser(loggedEmail, applicationName)
                            val prefs = getSharedPreferences("MySuccessPrefs", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("title", getString(R.string.request_sent))
                                putString("content", getString(R.string.request_message_sent))
                                putString("button", getString(R.string.show_history))
                                apply()
                            }
                            startActivity(Intent(this@AppointmentActivity, SuccessActivity::class.java))
                        } else {
                            Toast.makeText(this@AppointmentActivity, R.string.booking_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                        Toast.makeText(
                            this@AppointmentActivity,
                            getString(R.string.network_error, t.localizedMessage),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    // === Réseau / logique ===

    private fun fetchTakenSlots(user: String, application: String, onComplete: () -> Unit) {
        RetrofitClient.instance.getTakenSlots(user, application)
            .enqueue(object : Callback<TakenSlotsResponse> {
                override fun onResponse(call: Call<TakenSlotsResponse>, response: Response<TakenSlotsResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.status == "success") {
                            takenSlotsMap = body.taken_slots ?: emptyMap()
                            totalTechs = body.total_techs
                        } else {
                            Toast.makeText(this@AppointmentActivity, "Erreur serveur.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@AppointmentActivity, "Réponse inattendue: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                    onComplete()
                }
                override fun onFailure(call: Call<TakenSlotsResponse>, t: Throwable) {
                    Toast.makeText(this@AppointmentActivity, "Erreur de connexion: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            })
    }

    private fun generateUpcomingDays(count: Int): List<DayItem> {
        val list = mutableListOf<DayItem>()
        val calendar = Calendar.getInstance()
        val formatter = java.text.SimpleDateFormat("EEEE dd MMMM", Locale.ENGLISH)
        if (totalTechs <= 0) return emptyList()
        val maxScanDays = 365
        var scanned = 0
        while (list.size < count && scanned < maxScanDays) {
            val date = calendar.time
            val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val isWeekend = localDate.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            val isHoliday = isPublicHoliday(localDate)
            if (!isWeekend && !isHoliday) {
                val available = generateTimeSlots(date)
                if (available.isNotEmpty()) list.add(DayItem(formatter.format(date), available, localDate))
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            scanned++
        }
        return list
    }

    private fun generateTimeSlots(date: Date): List<String> {
        if (totalTechs <= 0) return emptyList()
        val calendar = Calendar.getInstance().apply { time = date }
        val format = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateKey = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        val takenForDate = takenSlotsMap[dateKey] ?: emptyMap()
        val slots = mutableListOf<String>()
        calendar.set(Calendar.HOUR_OF_DAY, 8)
        calendar.set(Calendar.MINUTE, 0)
        while (calendar.get(Calendar.HOUR_OF_DAY) <= 18) {
            val slot = format.format(calendar.time)
            val takenCount = takenForDate[slot] ?: 0
            if (takenCount < totalTechs) slots.add(slot)
            calendar.add(Calendar.HOUR_OF_DAY, 2)
        }
        return slots
    }

    private fun isPublicHoliday(date: LocalDate): Boolean {
        val fixed = setOf(
            LocalDate.of(date.year, 1, 1), LocalDate.of(date.year, 5, 1),
            LocalDate.of(date.year, 5, 8), LocalDate.of(date.year, 7, 14),
            LocalDate.of(date.year, 8, 15), LocalDate.of(date.year, 11, 1),
            LocalDate.of(date.year, 11, 11), LocalDate.of(date.year, 12, 25)
        )
        val easter = calculateEaster(date.year)
        return date in fixed || date in listOf(easter.plusDays(1), easter.plusDays(39), easter.plusDays(50))
    }

    private fun calculateEaster(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }

    private fun sendNotificationToAdmin(email: String, appName: String) {
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest("New request received", "admin", email, appName)
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun sendNotificationToUser(email: String, appName: String) {
        RetrofitClient.instance.notifyAdmin(
            NotificationRequest("New request sent successfully", "user", email, appName)
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    suspend fun fetchNearestTechnicianEmailSuspend(
        email: String,
        application: String,
        date: String,
        hourSlot: String
    ): String? = suspendCancellableCoroutine { cont ->
        RetrofitClient.instance.getNearestAdminEmail(
            TechnicianRequest(email, application, date, hourSlot)
        ).enqueue(object : Callback<TechnicianResponse> {
            override fun onResponse(call: Call<TechnicianResponse>, response: Response<TechnicianResponse>) {
                cont.resume(if (response.isSuccessful) response.body()?.email else null)
            }
            override fun onFailure(call: Call<TechnicianResponse>, t: Throwable) {
                cont.resume(null)
            }
        })
    }
}
