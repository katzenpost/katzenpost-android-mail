package com.fsck.k9.activity.setup


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.util.Pair
import android.transition.Fade
import android.view.View
import android.view.Window
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.bindView


class KatzenpostWelcomeActivity : K9Activity() {
    private val getStartedButton: View by bindView(R.id.button_next)
    private val logo: View by bindView(R.id.logo)

    private var shouldFinish = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTION_BAR)

        super.onCreate(savedInstanceState)

        val fade = Fade()
        fade.excludeTarget(android.R.id.statusBarBackground, true)
        fade.excludeTarget(android.R.id.navigationBarBackground, true)
        window.exitTransition = fade
        window.enterTransition = fade

        supportActionBar?.hide()

        setContentView(R.layout.katzenpost_welcome)

        getStartedButton.setOnClickListener {
            val setupActivityIntent = KatzenpostSetupActivity.getSetupActivityIntent(this)
            val sceneTransitionAnimation = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                    Pair.create(findViewById(android.R.id.statusBarBackground), Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME),
                    Pair.create(findViewById(android.R.id.navigationBarBackground), Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME),
                    Pair.create(logo, "logo")
            )
            startActivity(setupActivityIntent, sceneTransitionAnimation.toBundle())
        }
    }

    companion object {
        fun showWelcomeActivity(context: Context) {
            val intent = Intent(context, KatzenpostWelcomeActivity::class.java)
            context.startActivity(intent)
        }
    }
}
