package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
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


class KatzenpostSetupActivity : K9Activity() {
    private val rootView: ViewGroup by bindView(android.R.id.content)

    private val providerSpinner: Spinner by bindView(R.id.provider_select)
    private val finishButton: View by bindView(R.id.button_finish)
    private val nameLayout: ToolableViewAnimator by bindView(R.id.layout_katzen_name)
    private val nameText: TextView by bindView(R.id.katzenpost_name)
    private val nameRefreshButton: View by bindView(R.id.button_name_refresh)

    private val layoutStep1: View by bindView(R.id.layout_step_1)
    private val layoutStep2: View by bindView(R.id.layout_step_2)
    private val layoutStep3: View by bindView(R.id.layout_step_3)

    private var state: State = State.SELECT_PROVIDER
    private var providerName: String? = null

    private val providerAdapter: ArrayAdapter<String>
        get() {
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter.add("(select provider)")
            adapter.add("pano")
            adapter.add("ramix")
            adapter.add("idefix")
            return adapter
        }

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

    private fun onClickFinish() {
        Toast.makeText(this, "Ok", Toast.LENGTH_LONG).show()
    }

    private fun displayStateSelectProvider() {
        val animStyle = if (state > State.SELECT_PROVIDER) fadeQuick else fade
        state = State.SELECT_PROVIDER

        providerSpinner.isEnabled = true

        TransitionManager.beginDelayedTransition(rootView, animStyle)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.GONE
        layoutStep3.visibility = View.GONE
    }

    private fun displayStateLoadName() {
        val animStyle = if (state > State.LOAD_NAME) fadeQuick else fade
        state = State.LOAD_NAME

        providerSpinner.isEnabled = false
        nameLayout.setDisplayedChild(0, false)

        TransitionManager.beginDelayedTransition(rootView, animStyle)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.VISIBLE
        layoutStep3.visibility = View.GONE
    }

    private fun displayStateDone() {
        state = State.DONE

        providerSpinner.isEnabled = false

        TransitionManager.beginDelayedTransition(rootView, fadeQuick)
        layoutStep1.visibility = View.VISIBLE
        layoutStep2.visibility = View.VISIBLE
        layoutStep3.visibility = View.VISIBLE
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
        displayStateLoadName()

        Handler().postDelayed({
            onLoadNameFinished("WildAnaconda@${providerName}")
        }, 1500)
    }

    private fun onLoadNameFinished(name: String) {
        nameText.text = name
        nameLayout.displayedChild = 1
        displayStateDone()
    }

    override fun onBackPressed() {
        when (state) {
            State.SELECT_PROVIDER -> super.onBackPressed()
            State.LOAD_NAME -> displayStateSelectProvider()
            State.DONE -> displayStateSelectProvider()
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

    enum class State {
        SELECT_PROVIDER, LOAD_NAME, DONE
    }
}
