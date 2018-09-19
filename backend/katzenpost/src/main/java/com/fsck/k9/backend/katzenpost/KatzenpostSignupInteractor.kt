package com.fsck.k9.backend.katzenpost

import katzenpost.Katzenpost
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.*

class KatzenpostSignupInteractor {
    private val httpClient = OkHttpClient()

    fun getProviderNames() = PROVIDER_HOSTS.keys

    fun requestNameReservation(providerName: String): NameReservationToken {
        Thread.sleep(1000)
        return NameReservationToken(providerName, "LazyBones" + Random().nextInt(1000))
    }

    @Throws(RegistrationException::class)
    fun requestCreateAccount(reservationToken: NameReservationToken): KatzenpostServerSettings {
        val linkkey = Katzenpost.genKey()
        val idkey = Katzenpost.genKey()

        doRegistrationRequest(reservationToken, linkkey.public, idkey.public)

        Thread.sleep(1000)
        return KatzenpostServerSettings(reservationToken.provider, reservationToken.name, linkkey.private, idkey.private)
    }

    @Throws(RegistrationException::class)
    private fun doRegistrationRequest(reservationToken: NameReservationToken, linkkey: String, idkey: String): Response {
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("version", "0")
                .addFormDataPart("command", "register_link_and_identity_key")
                .addFormDataPart("user", reservationToken.name)
                .addFormDataPart("link_key", linkkey)
                .addFormDataPart("identity_key", idkey)
                .build()

        val request = Request.Builder()
                .url(PROVIDER_HOSTS.getValue(reservationToken.provider))
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
        private val PROVIDER_HOSTS = mapOf(
                "playground" to "https://playground.katzenpost.mixnetworks.org:61532/registration"
        )
    }
}

data class NameReservationToken(val provider: String, val name: String)

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
