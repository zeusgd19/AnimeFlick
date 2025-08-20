package com.zeusgd.AnimeFlick

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ocultar la barra de estado
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoIzquierda: ImageView = findViewById(R.id.logo_izquierda)
        val logoDerecha: ImageView = findViewById(R.id.logo_derecha)
        val logoAbajo: ImageView = findViewById(R.id.logo_abajo)

        logoDerecha.scaleX = 2f
        logoDerecha.scaleY = 2f
        logoIzquierda.scaleX = 2f
        logoIzquierda.scaleY = 2f
        logoAbajo.scaleX = 2f
        logoAbajo.scaleY = 2f

        // Posiciones iniciales
        logoDerecha.translationX = 3000f
        logoDerecha.translationY = 6000f

        logoIzquierda.translationX = -3000f
        logoIzquierda.translationY = 5000f

        logoAbajo.translationX = -4000f
        logoAbajo.translationY = 2000f

        val customInterpolator = PathInterpolator(0.8f, 0f, 0.2f, 1f)

        // Animación derecha → centro
        val animRight = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logoDerecha, View.TRANSLATION_X, 0f),
                ObjectAnimator.ofFloat(logoDerecha, View.TRANSLATION_Y, 0f)
            )
            interpolator = customInterpolator
            duration = 500
        }

        // Animación izquierda → centro
        val animLeft = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logoIzquierda, View.TRANSLATION_X, 0f),
                ObjectAnimator.ofFloat(logoIzquierda, View.TRANSLATION_Y, 0f)
            )
            interpolator = customInterpolator
            duration = 300
        }

        // Animación abajo → centro
        val animBottom = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logoAbajo, View.TRANSLATION_Y, 0f),
                ObjectAnimator.ofFloat(logoAbajo, View.TRANSLATION_X, 0f)
            )
            interpolator = customInterpolator
            duration = 300
        }

        // Secuencia total
        val sequence = AnimatorSet().apply {
            play(animRight)
            play(animLeft).after(animRight)
            play(animBottom).after(animLeft)
        }

        sequence.start()

        // Ir al MainActivity después
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}
