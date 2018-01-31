package com.fsck.k9.activity.setup

import com.fsck.k9.mail.store.katzenpost.KatzenpostServerSettings

class KatzenpostSignupInteractor {
    fun getProviderNames(): List<String> = listOf("pano", "ramix", "idefix")

    fun reserveName(providerName: String): NameReservationToken {
        Thread.sleep(1500)

        // TODO

        return NameReservationToken(providerName, "WildAnaconda", "abc")
    }

    fun finishSignup(reservationToken: NameReservationToken): KatzenpostServerSettings {
        Thread.sleep(1500)

        return KatzenpostServerSettings("ramix", "eve", "97f906cc6acd1ab84d3e66cfa6c1526febaa5d0cc73342def908dd2197aad6f4")
    }

}

data class NameReservationToken(val provider: String, val name: String, val token: String)