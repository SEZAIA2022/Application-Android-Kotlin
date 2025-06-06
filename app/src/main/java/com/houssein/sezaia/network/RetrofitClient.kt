package com.houssein.sezaia.network


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://172.20.10.5:5000/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}


// pour nettoyer le build de l'application en cas d'erreurs, utiliser dans powershell ce commande:  Remove-Item -Recurse -Force "C:\Users\hsein\Sezaia App\Application-Android-Kotlin\app\build"
