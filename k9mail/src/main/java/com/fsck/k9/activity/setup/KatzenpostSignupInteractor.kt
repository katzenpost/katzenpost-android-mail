package com.fsck.k9.activity.setup

class KatzenpostSignupInteractor {
    fun reserveName(providerName: String): NameReservationToken {
        Thread.sleep(1500)

        // TODO

        return NameReservationToken(providerName, "WildAnaconda", "abc")
    }

    fun finishSignup(reservationToken: NameReservationToken): Boolean {
        Thread.sleep(1500)

        return true
    }

}

data class NameReservationToken(val provider: String, val name: String, val token: String)