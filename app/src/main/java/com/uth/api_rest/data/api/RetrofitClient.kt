package com.uth.api_rest.data.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val PREFS_NAME = "api_rest_prefs"
    private const val KEY_BASE_URL = "base_url"
    const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/"

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String = DEFAULT_BASE_URL

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBaseUrl(context: Context): String {
        return getPreferences(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        var formattedUrl = url.trim()
        if (!formattedUrl.endsWith("/")) {
            formattedUrl += "/"
        }
        getPreferences(context).edit {
            putString(KEY_BASE_URL, formattedUrl)
        }
        retrofit = null
    }

    fun getApiService(context: Context): ProductoApiService {
        val baseUrl = getBaseUrl(context)
        if (retrofit == null || (currentBaseUrl != baseUrl)) {
            currentBaseUrl = baseUrl

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ProductoApiService::class.java)
    }
}
