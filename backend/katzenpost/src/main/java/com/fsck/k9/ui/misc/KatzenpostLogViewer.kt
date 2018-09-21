package com.fsck.k9.ui.misc

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.fsck.k9.backend.katzenpost.R

class KatzenpostLogViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_katzenpost_log_viewer)
    }
}