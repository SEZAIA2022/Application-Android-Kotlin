package com.houssein.sezaia.model.data

import android.app.Application
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MyApp : Application() {

    companion object {
        private const val TAG = "MyApp"
        private const val PREFS = "LoginData"
        private const val KEY_APP_NAME = "application_name"
        private const val KEY_APP_TYPE = "application_type"

        private const val DEFAULT_APP_NAME = ""
        private const val DEFAULT_APP_TYPE = "scan"
    }

    // --- Flows (source de vérité réactive)
    private val _applicationNameFlow = MutableStateFlow(DEFAULT_APP_NAME)
    val applicationNameFlow: StateFlow<String> = _applicationNameFlow

    private val _applicationTypeFlow = MutableStateFlow(DEFAULT_APP_TYPE)
    val applicationTypeFlow: StateFlow<String> = _applicationTypeFlow

    private val _appInfoLoadedFlow = MutableStateFlow(false)
    val appInfoLoadedFlow: StateFlow<Boolean> = _appInfoLoadedFlow

    // --- Snapshots legacy (compat)
    @Volatile var application_name: String = DEFAULT_APP_NAME
        private set
    @Volatile var application_type: String = DEFAULT_APP_TYPE
        private set
    @Volatile var isAppInfoLoaded: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        // Charger depuis le cache au démarrage (pas d'appel réseau ici)
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val cachedName = prefs.getString(KEY_APP_NAME, DEFAULT_APP_NAME) ?: DEFAULT_APP_NAME
        val cachedType = prefs.getString(KEY_APP_TYPE, DEFAULT_APP_TYPE) ?: DEFAULT_APP_TYPE
        setApplicationInfo(cachedName, cachedType, from = "cache")
        markLoaded()
    }

    /** Setter centralisé (vars + flows + prefs) */
    fun setApplicationInfo(name: String, type: String, from: String = "manual") {
        val safeName = name.ifBlank { DEFAULT_APP_NAME }
        val safeType = type.ifBlank { DEFAULT_APP_TYPE }

        application_name = safeName
        application_type = safeType
        _applicationNameFlow.value = safeName
        _applicationTypeFlow.value = safeType

        getSharedPreferences(PREFS, MODE_PRIVATE).edit {
            putString(KEY_APP_NAME, safeName)
            putString(KEY_APP_TYPE, safeType)
        }

        Log.d(TAG, "App=$safeName, Type=$safeType (source=$from)")
    }

    /** Indique que l’info est chargée (vars + flow) */
    private fun markLoaded() {
        isAppInfoLoaded = true
        _appInfoLoadedFlow.value = true
    }
}
