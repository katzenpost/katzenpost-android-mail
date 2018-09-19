package com.fsck.k9.activity.setup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.fsck.k9.Account
import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.katzenpost.KatzenpostServerSettings
import com.fsck.k9.backend.katzenpost.KatzenpostSignupInteractor
import com.fsck.k9.backend.katzenpost.NameReservationToken
import com.fsck.k9.backend.katzenpost.RegistrationException
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.ui.R
import kotlinx.android.synthetic.main.katzenpost_setup.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.koin.android.ext.android.inject
import timber.log.Timber


@SuppressLint("StaticFieldLeak")
class KatzenpostSetupActivity : K9Activity() {
    private val signupInteractor = KatzenpostSignupInteractor()
    private val backendManager by inject<BackendManager>()

    private val rootView by lazy {
        findViewById<ViewGroup>(android.R.id.content)
    }

    private val providerAdapter: ArrayAdapter<String> by lazy {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapter.add("(select provider)")
        for (provider in signupInteractor.getProviderNames()) {
            adapter.add(provider)
        }
        adapter
    }

    enum class State {
        INIT, SELECT_PROVIDER, RESERVE_LOADING, RESERVE_DONE, REGISTER_LOADING, REGISTER_DONE
    }
    private var state: State = State.INIT
    private var providerName: String? = null
    private var reservationToken: NameReservationToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTION_BAR)

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContentView(R.layout.katzenpost_setup)

        val fade = android.transition.Fade()
        fade.excludeTarget(android.R.id.statusBarBackground, true)
        fade.excludeTarget(android.R.id.navigationBarBackground, true)

        window.exitTransition = fade
        window.enterTransition = fade

        spinnerProviderSelect.adapter = providerAdapter

        spinnerProviderSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = onSelectProvider(0)
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
                    onSelectProvider(position)
        }

        buttonNameRefresh.setOnClickListener { onClickRefreshName() }
        buttonRegister.setOnClickListener { onClickFinish() }

        displayStateSelectProvider()
    }

    private fun displayStateSelectProvider() {
        val animStyle = if (state == State.INIT) fade else fadeQuick
        state = State.SELECT_PROVIDER

        TransitionManager.beginDelayedTransition(rootView, animStyle)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.GONE
        layoutStep3.visibility = View.GONE

        spinnerProviderSelect.isEnabled = true
    }

    private fun displayStateReserveLoading() {
        val animStyle = if (state == State.SELECT_PROVIDER) fade else fadeQuick
        state = State.RESERVE_LOADING

        layoutKatzenName.setDisplayedChild(0, false)

        TransitionManager.beginDelayedTransition(rootView, animStyle)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.VISIBLE
        layoutStep3.visibility = View.GONE

        spinnerProviderSelect.isEnabled = false
    }

    private fun displayStateReserveDone() {
        state = State.RESERVE_DONE

        layoutKatzenName.displayedChild = 1

        TransitionManager.beginDelayedTransition(rootView, fade)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.VISIBLE
        layoutStep3.visibility = View.VISIBLE

        buttonNameRefresh.isEnabled = true
        buttonRegister.isEnabled = true
        spinnerProviderSelect.isEnabled = false
    }

    private fun displayStateRegisterLoading() {
        if (state != State.RESERVE_DONE) return
        state = State.REGISTER_LOADING

        buttonNameRefresh.isEnabled = false
        buttonRegister.isEnabled = false
        progressRegister.visibility = View.VISIBLE
    }

    private fun onSelectProvider(position: Int) {
        if (position == 0) {
            return
        }

        providerName = providerAdapter.getItem(position)
        loadName()
    }

    private fun onClickRefreshName() {
        loadName()
    }

    private fun loadName() {
        displayStateReserveLoading()

        object : AsyncTaskEx<Void,Void,NameReservationToken,RegistrationException>() {
            override fun doInBackgroundEx(vararg params: Void?): NameReservationToken {
                return signupInteractor.requestNameReservation(providerName!!)
            }
            override fun onPostExecuteEx(exception: RegistrationException) {
                onLoadNameFailed(exception)
            }
            override fun onPostExecuteEx(reservationToken: NameReservationToken) {
                onLoadNameFinished(reservationToken)
            }
        }.execute()
    }

    private fun onLoadNameFailed(exception: RegistrationException) {
        Timber.e(exception, "Error reserving Katzenpost name!")
        if (state != State.RESERVE_LOADING) {
            return
        }

//        val snackbar = Snackbar.with(applicationContext)
//        snackbar.text(exception.message)
//        SnackbarManager.show(snackbar, this)

        this.reservationToken = null

        textKatzenpostName.text = "Error"
        layoutKatzenName.displayedChild = 1
        buttonNameRefresh.isEnabled = true
    }

    private fun onLoadNameFinished(reservationToken: NameReservationToken) {
        if (state != State.RESERVE_LOADING) {
            return
        }

        this.reservationToken = reservationToken

        textKatzenpostName.text = "${reservationToken.name}@${reservationToken.provider}"

        displayStateReserveDone()
    }

    private fun onClickFinish() {
        displayStateRegisterLoading()

        object : AsyncTaskEx<Void, Void, KatzenpostServerSettings, RegistrationException>() {
            override fun doInBackgroundEx(vararg params: Void?) = signupInteractor.requestCreateAccount(reservationToken!!)
            override fun onPostExecuteEx(result: KatzenpostServerSettings) = onRegisterComplete(result)
            override fun onPostExecuteEx(e: RegistrationException?) = onRegisterFailed(exception)
        }.execute()
    }

    private fun onRegisterComplete(serverSettings: KatzenpostServerSettings) {
        state = State.REGISTER_DONE
        progressRegister.visibility = View.GONE

        launch (UI) {
            val bg = bg { saveAccount(serverSettings) }

            delay(1000)
            bg.await()

            window.sharedElementReturnTransition = null
            window.sharedElementReenterTransition = null
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun onRegisterFailed(exception: RegistrationException) {
        Timber.e(exception, "Error reserving Katzenpost name!")
        if (state != State.REGISTER_LOADING) {
            return
        }

        Toast.makeText(this, "error", Toast.LENGTH_SHORT).show()

//        val snackbar = Snackbar.with(applicationContext)
//        snackbar.text(exception.message)
//        SnackbarManager.show(snackbar, this)
    }

    private fun saveAccount(serverSettings: KatzenpostServerSettings) {
        val preferences = Preferences.getPreferences(this)
        val account = preferences.newAccount()
        account.name = serverSettings.name
        account.email = serverSettings.address
        account.description = serverSettings.address

        account.storeUri = backendManager.createStoreUri(serverSettings)
        account.transportUri = backendManager.createTransportUri(serverSettings)

        setupFolderNames(account)
        account.deletePolicy = Account.DeletePolicy.ON_DELETE

        account.save(preferences)

        val messagingController = MessagingController.getInstance(application)
        messagingController.listFoldersSynchronous(account, true, null)
        messagingController.synchronizeMailbox(account, account.inboxFolder, null, null)

        Core.setServicesEnabled(this)
    }

    private fun setupFolderNames(account: Account) {
        account.inboxFolder = getString(R.string.special_mailbox_name_inbox)
        account.draftsFolder = getString(R.string.special_mailbox_name_drafts)
        account.trashFolder = getString(R.string.special_mailbox_name_trash)
        account.sentFolder = getString(R.string.special_mailbox_name_sent)
        account.archiveFolder = getString(R.string.special_mailbox_name_archive)
        account.spamFolder = getString(R.string.special_mailbox_name_spam)
    }

    override fun onBackPressed() {
        when (state) {
            State.INIT, State.SELECT_PROVIDER -> super.onBackPressed()
            State.RESERVE_LOADING, State.REGISTER_LOADING -> return
            State.RESERVE_DONE -> displayStateSelectProvider()
            State.REGISTER_DONE -> return
        }
    }

    companion object {
        val fade: Transition by lazy {
            val fade = Fade()
            fade.startDelay = 500
            fade
        }
        val fadeQuick = Fade()

        fun getSetupActivityIntent(context: Context): Intent {
            return Intent(context, KatzenpostSetupActivity::class.java)
        }
    }
}
