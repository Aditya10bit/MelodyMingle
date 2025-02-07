package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivitySplashScreenBinding
import com.google.firebase.auth.FirebaseAuth

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding
    private var isFirstLaunch = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Only show splash screen on first launch
        if (isFirstLaunch) {
            binding.lottieanimationview.setAnimation(R.raw.music)
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToNextScreen()
                isFirstLaunch = false
            }, 3000)
        } else {
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        val intent = if (FirebaseAuth.getInstance().currentUser == null) {
            Intent(this, LoginActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
