package com.zeusgd.AnimeFlick

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        playerView = findViewById(R.id.player_view)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player


        val videoUrl = intent.getStringExtra("videoUrl")
        val headers = intent.getSerializableExtra("headers") as? HashMap<String, String> ?: hashMapOf()

        if (videoUrl != null) {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setCustomCacheKey("anime_stream")
                .setTag("streamwish")
                .build()

            val mediaSource = com.google.android.exoplayer2.source.ProgressiveMediaSource.Factory(
                com.google.android.exoplayer2.upstream.DefaultHttpDataSource.Factory().apply {
                    setDefaultRequestProperties(headers)
                }
            ).createMediaSource(mediaItem)

            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
        }
    }

    override fun onStop() {
        super.onStop()
        player.release()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        supportActionBar?.hide()
    }
}
