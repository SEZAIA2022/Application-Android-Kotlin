// com/houssein/sezaia/model/data/MyApp.kt
package com.houssein.sezaia.model.data

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import com.houssein.sezaia.model.response.AppNameTypeResponse
import com.houssein.sezaia.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyApp : Application() {

    companion object {
        private const val TAG = "MyApp"
        private const val PREFS = "LoginData"
        private const val KEY_APP_NAME = "application_name"
        private const val KEY_APP_TYPE = "application_type"

        private const val DEFAULT_APP_NAME = "sezaia"
        private const val DEFAULT_APP_TYPE = "scan"
    }

    // --- Flows (source de vérité réactive)
    private val _applicationNameFlow = MutableStateFlow(DEFAULT_APP_NAME)
    val applicationNameFlow: StateFlow<String> = _applicationNameFlow

    private val _applicationTypeFlow = MutableStateFlow(DEFAULT_APP_TYPE)
    val applicationTypeFlow: StateFlow<String> = _applicationTypeFlow

    private val _appInfoLoadedFlow = MutableStateFlow(false)
    val appInfoLoadedFlow: StateFlow<Boolean> = _appInfoLoadedFlow

    // --- Snapshots legacy (compatibilité avec l’existant)
    @Volatile var application_name: String = DEFAULT_APP_NAME
        private set
    @Volatile var application_type: String = DEFAULT_APP_TYPE
        private set
    @Volatile var isAppInfoLoaded: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()

        // Puis rafraîchir depuis l’API
        fetchAppNameType()
    }

    /** Setter centralisé (vars + flows + prefs) */
    fun setApplicationInfo(name: String, type: String, from: String = "manual") {
        application_name = name
        application_type = type
        _applicationNameFlow.value = name
        _applicationTypeFlow.value = type

        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit {
                putString(KEY_APP_NAME, name)
                    .putString(KEY_APP_TYPE, type)
            }

        Log.d(TAG, "App=$name, Type=$type (source=$from)")
    }



    /** Indique que l’info est chargée (vars + flow) */
    private fun markLoaded() {
        isAppInfoLoaded = true
        _appInfoLoadedFlow.value = true
    }

    /** Récupération réseau et MAJ cohérente */
    fun fetchAppNameType() {
        val nameToQuery = application_name.ifBlank { DEFAULT_APP_NAME }

        RetrofitClient.instance.getAppNameType(nameToQuery)
            .enqueue(object : Callback<AppNameTypeResponse> {
                override fun onResponse(
                    call: Call<AppNameTypeResponse>,
                    response: Response<AppNameTypeResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "HTTP ${response.code()} ${response.message()}")
                        markLoaded()
                        return
                    }
                    val body = response.body()
                    if (body?.status == "success" && body.data != null) {
                        val newName = body.data.application?.toString()?.ifBlank { DEFAULT_APP_NAME } ?: DEFAULT_APP_NAME
                        val newType = body.data.type?.toString()?.ifBlank { DEFAULT_APP_TYPE } ?: DEFAULT_APP_TYPE
                        setApplicationInfo(newName, newType, from = "api")
                    } else {
                        Log.w(TAG, "Données introuvables pour application_name=$nameToQuery")
                    }
                    markLoaded()
                }

                override fun onFailure(call: Call<AppNameTypeResponse>, t: Throwable) {
                    Log.e(TAG, "Échec API: ${t.message}", t)
                    markLoaded()
                }
            })
    }
}
