package com.example.tharo_app_food.Activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tharo_app_food.R

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        handler.postDelayed({
            checkLoginStatus()
        }, 5000)
    }

    private fun checkLoginStatus() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val userRole = prefs.getString("user_role", null)

        if (isLoggedIn && userRole != null) {
            when (userRole) {
                "Admin" -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                }
                else -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        } else {
            // Nếu chưa đăng nhập thì vào MainActivity hoặc LoginActivity tùy ý bạn
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
