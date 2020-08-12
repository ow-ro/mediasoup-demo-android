package org.mediasoup.droid.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.postDelayed
import kotlinx.android.synthetic.main.activity_splash_screen.*

class SplashScreenActivity : AppCompatActivity(R.layout.activity_splash_screen) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediasoup.postDelayed(1000) {
            startActivity(Intent(this, RoomActivity::class.java))
            ActivityCompat.finishAfterTransition(this)
        }
    }
}