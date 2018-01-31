package com.fsck.k9.activity.setup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.bindView
import com.fsck.k9.view.ToolableViewAnimator
import com.transitionseverywhere.Fade
import com.transitionseverywhere.Transition
import com.transitionseverywhere.TransitionManager


@SuppressLint("StaticFieldLeak")
class KatzenpostSetupActivity : K9Activity() {
    private val rootView: ViewGroup by bindView(android.R.id.content)

    private val providerSpinner: Spinner by bindView(R.id.provider_select)
    private val finishButton: View by bindView(R.id.button_register)
    private val nameLayout: ToolableViewAnimator by bindView(R.id.layout_katzen_name)
    private val nameText: TextView by bindView(R.id.katzenpost_name)
    private val nameRefreshButton: View by bindView(R.id.button_name_refresh)
    private val progressRegister: View by bindView(R.id.progress_register)

    private val layoutStep1: View by bindView(R.id.layout_step_1)
    private val layoutStep2: View by bindView(R.id.layout_step_2)
    private val layoutStep3: View by bindView(R.id.layout_step_3)

    private val signupInteractor = KatzenpostSignupInteractor()

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

        providerSpinner.adapter = providerAdapter

        providerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = onSelectProvider(0)
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
                    onSelectProvider(position)
        }

        nameRefreshButton.setOnClickListener { onClickRefreshName() }
        finishButton.setOnClickListener { onClickFinish() }

        displayStateSelectProvider()
    }

    private fun displayStateSelectProvider() {
        val animStyle = if (state == State.INIT) fade else fadeQuick
        state = State.SELECT_PROVIDER

        TransitionManager.beginDelayedTransition(rootView, animStyle)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.GONE
        layoutStep3.visibility = View.GONE

        providerSpinner.isEnabled = true
    }

    private fun displayStateReserveLoading() {
        val animStyle = if (state == State.SELECT_PROVIDER) fade else fadeQuick
        state = State.RESERVE_LOADING

        nameLayout.setDisplayedChild(0, false)

        TransitionManager.beginDelayedTransition(rootView, animStyle)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.VISIBLE
        layoutStep3.visibility = View.GONE

        providerSpinner.isEnabled = false
    }

    private fun displayStateReserveDone() {
        state = State.RESERVE_DONE

        nameLayout.displayedChild = 1

        TransitionManager.beginDelayedTransition(rootView, fade)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.VISIBLE
        layoutStep3.visibility = View.VISIBLE

        nameRefreshButton.isEnabled = true
        finishButton.isEnabled = true
        providerSpinner.isEnabled = false
    }

    private fun displayStateRegisterLoading() {
        if (state != State.RESERVE_DONE) return
        state = State.REGISTER_LOADING

        nameRefreshButton.isEnabled = false
        finishButton.isEnabled = false
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

        object : AsyncTask<Void,Void, NameReservationToken>() {
            override fun doInBackground(vararg params: Void?) = signupInteractor.reserveName(providerName!!)
            override fun onPostExecute(reservationToken: NameReservationToken) = onLoadNameFinished(reservationToken)
        }.execute()
    }

    private fun onLoadNameFinished(reservationToken: NameReservationToken) {
        if (state != State.RESERVE_LOADING) {
            return
        }

        this.reservationToken = reservationToken

        nameText.text = "${reservationToken.name}@${reservationToken.provider}"

        displayStateReserveDone()
    }

    private fun onClickFinish() {
        displayStateRegisterLoading()

        object : AsyncTask<Void,Void,Boolean>() {
            override fun doInBackground(vararg params: Void?) = signupInteractor.finishSignup(reservationToken!!)
            override fun onPostExecute(result: Boolean) {
                state = State.REGISTER_DONE
                progressRegister.visibility = View.GONE

                Toast.makeText(this@KatzenpostSetupActivity, "xxx", Toast.LENGTH_LONG).show()
                super.onPostExecute(result)
            }
        }.execute()
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
