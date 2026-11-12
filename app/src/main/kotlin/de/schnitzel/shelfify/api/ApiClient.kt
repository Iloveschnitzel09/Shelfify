package de.schnitzel.shelfify.api

import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            try {
                chain.proceed(chain.request())
            } catch (e: IOException) {
                ConnectionGuard.reportConnectionFailure()
                throw e
            }
        }
        .build()
}