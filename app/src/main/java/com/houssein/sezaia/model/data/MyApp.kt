// com/houssein/sezaia/model/data/MyApp.kt
package com.houssein.sezaia.model.data

import android.app.Application
import android.util.Log
import com.houssein.sezaia.model.response.AppNameTypeResponse
import com.houssein.sezaia.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyApp : Application() {


    // Données récupérées depuis l’API accessibles partout via (applicationContext as MyApp)
    @Volatile var application_name: String = "sezaia"
    @Volatile var application_type: String = "scan"
    @Volatile var isAppInfoLoaded: Boolean = false

    override fun onCreate() {
        super.onCreate()
        fetchAppNameType()   // appel dès le lancement
    }

    private fun fetchAppNameType() {
        RetrofitClient.instance.getAppNameType(application_name)
            .enqueue(object : Callback<AppNameTypeResponse> {
                override fun onResponse(
                    call: Call<AppNameTypeResponse>,
                    response: Response<AppNameTypeResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.e("MyApp", "HTTP ${response.code()} ${response.message()}")
                        isAppInfoLoaded = true
                        return
                    }
                    val body = response.body()
                    if (body?.status == "success" && body.data != null) {
                        application_name = body.data.application.toString()
                        application_type = body.data.type.toString()
                        Log.d("MyApp", "App=${application_name}, Type=${application_type}")
                    } else {
                        Log.w("MyApp", "Données introuvables pour application_name=$application_name")
                    }
                    isAppInfoLoaded = true
                }

                override fun onFailure(call: Call<AppNameTypeResponse>, t: Throwable) {
                    Log.e("MyApp", "Échec API: ${t.message}", t)
                    isAppInfoLoaded = true
                }
            })
    }
}
