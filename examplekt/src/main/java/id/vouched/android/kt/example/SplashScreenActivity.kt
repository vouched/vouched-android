package id.vouched.android.kt.example

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashScreenActivity : AppCompatActivity(R.layout.activity_splash_screen) {

    companion object {
        const val SPLASH_SCREEN_DURATION = 2000L
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(SPLASH_SCREEN_DURATION)
            val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
