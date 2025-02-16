package com.example.karaoke

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    // Override onTouchEvent to detect touch and transition immediately
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // When the screen is touched, immediately transition to the next activity
        if (event != null) {
            val mainIntent = Intent(this, SongListActivity::class.java)
            startActivity(mainIntent)
            finish()
            return true // Return true to indicate the event was handled
        }
        return super.onTouchEvent(event)
    }
}
