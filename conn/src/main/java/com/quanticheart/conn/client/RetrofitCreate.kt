package com.quanticheart.conn.client

import android.annotation.SuppressLint
import com.quanticheart.conn.config.ApiConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException

object RetrofitCreate {

    inline fun <reified T> createConn(apiConfig: ApiConfig): T {
        val header = createHeader(apiConfig)
        val r = Retrofit.Builder()
            .baseUrl(apiConfig.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(header)
            .build()

        return r.create(T::class.java)
    }

    fun createHeader(connectionModel: ApiConfig): OkHttpClient {
        val client: OkHttpClient.Builder = OkHttpClient.Builder()
        val interceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
            connectionModel.header.forEach { map ->
                request.addHeader(map.key, map.value)
            }
            chain.proceed(request.build())
        }
        setOkHTTPSettings(client, interceptor, connectionModel)
        setSllConfig(client)
        return client.build()
    }

    private fun setOkHTTPSettings(
        client: OkHttpClient.Builder,
        interceptorWithHeaders: Interceptor,
        connectionModel: ApiConfig
    ) {
        client.addInterceptor(interceptorWithHeaders)
        client.connectTimeout(connectionModel.connectionTimeOutMin.toLong(), TimeUnit.MINUTES)
        client.writeTimeout(connectionModel.connectionTimeOutMin.toLong(), TimeUnit.MINUTES)
        client.readTimeout(connectionModel.connectionTimeOutMin.toLong(), TimeUnit.MINUTES)

        val log = HttpLoggingInterceptor()
        when (connectionModel.consoleLog) {
            true -> log.level = HttpLoggingInterceptor.Level.BODY
            false -> log.level = HttpLoggingInterceptor.Level.NONE
        }
        client.addInterceptor(log)
    }

    private fun setSllConfig(client: OkHttpClient.Builder) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return emptyArray()
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                }
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            client.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            client.hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}