package com.zeusgd.AnimeFlick

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var logoA: ImageView
    private lateinit var logoText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logoA = findViewById(R.id.logoA)
        logoText = findViewById(R.id.logoText)

        logoA.visibility = View.VISIBLE
        logoA.scaleX = 10f
        logoA.scaleY = 10f
        logoText.scaleX = 3f
        logoText.scaleY = 3f
        val shrink = ObjectAnimator.ofPropertyValuesHolder(
            logoA,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
        ).apply {
            duration = 600
        }

        val rotateFast = ObjectAnimator.ofFloat(logoA, View.ROTATION, 0f, 1080f).apply {
            duration = 1000
        }

        val moveLeft = ObjectAnimator.ofFloat(logoA, View.TRANSLATION_X, 0f, -270f).apply {
            duration = 500
        }

        val textSlide = ObjectAnimator.ofFloat(logoText, View.TRANSLATION_X, -400f, -270f).apply {
            duration = 600
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    logoText.visibility = View.VISIBLE
                }
            })
        }

        val allAnim = AnimatorSet()
        allAnim.playSequentially(shrink, rotateFast, moveLeft, textSlide)
        allAnim.start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }
}

