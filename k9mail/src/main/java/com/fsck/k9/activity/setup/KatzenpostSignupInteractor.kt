package com.fsck.k9.activity.setup

import com.fsck.k9.mail.store.katzenpost.KatzenpostServerSettings
import katzenpost.Katzenpost
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class KatzenpostSignupInteractor {
    private val httpClient = OkHttpClient()

    fun getProviderNames(): List<String> = listOf("pano", "ramix", "idefix")

    fun requestNameReservation(providerName: String): NameReservationToken {
        val response: String = doRequestReservation(providerName)
        val (username, token) = parseUsernameAndToken(response)

        Thread.sleep(1000)
        return NameReservationToken(providerName, username, token)
    }

    fun requestCreateAccount(reservationToken: NameReservationToken): KatzenpostServerSettings {
        val linkkey = Katzenpost.genKey()
        val idkey = Katzenpost.genKey()

        doRegistrationRequest(reservationToken, linkkey.public, idkey.public)

        Thread.sleep(1000)
        return KatzenpostServerSettings(reservationToken.provider, reservationToken.name, linkkey.private, idkey.private)
    }

    private fun doRequestReservation(providerName: String): String {
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("pre", "1")
                .build()

        val request = Request.Builder()
                .url(PROVIDER_REGISTRATION_URLS[providerName]!!)
                .post(requestBody)
                .build()

        try {
            val body = httpClient.newCall(request).execute().body()
            if (body != null) {
                return body.string()
            }
            throw RegistrationResponseException("Received empty response!")
        } catch (e: IOException) {
            throw RegistrationNetworkException(e)
        }
    }

    private fun parseUsernameAndToken(responseStr: String): Pair<String, String> {
        try {
            val response = JSONObject(responseStr)

            val username = response.getString("username")
            val token = response.getString("token")

            return Pair(username, token)
        } catch (e: JSONException) {
            throw RegistrationResponseException("Error parsing response!", e)
        }
    }

    private fun doRegistrationRequest(reservationToken: NameReservationToken, linkkey: String, idkey: String): Response {
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("token", reservationToken.token)
                .addFormDataPart("linkkey", linkkey)
                .addFormDataPart("idkey", idkey)
                .build()

        val request = Request.Builder()
                .url(PROVIDER_REGISTRATION_URLS[reservationToken.provider]!!)
                .post(requestBody)
                .build()

        try {
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw RegistrationResponseException("Got negative response: ${response.code()}")
            }

            return response
        } catch (e: IOException) {
            throw RegistrationNetworkException(e)
        }
    }

    companion object {
        private val PROVIDER_REGISTRATION_URLS = mapOf(
                "pano" to "http://37.218.240.163:7900/register",
                "ramix" to "http://199.119.112.92:7900/register",
                "idefix" to "http://217.197.90.63:7900/register"
        )
    }
}

data class NameReservationToken(val provider: String, val name: String, val token: String)

abstract class RegistrationException : Exception {
    constructor(message: String, e: Exception) : super(message, e)
    constructor(message: String) : super(message)
    constructor(e: IOException) : super(e)
}

class RegistrationNetworkException(e: IOException) : RegistrationException(e)
class RegistrationResponseException : RegistrationException {
    constructor(message: String) : super(message)
    constructor(message: String, e: Exception) : super(message, e)
}
