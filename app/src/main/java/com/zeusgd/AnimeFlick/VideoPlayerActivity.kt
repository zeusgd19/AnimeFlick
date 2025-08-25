package com.zeusgd.AnimeFlick

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet.Layout
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.zeusgd.AnimeFlick.viewmodel.AnimeViewModel
import kotlinx.coroutines.launch

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var player: ExoPlayer
    private var currentSlug: String = ""
    private var server: String = "";
    private val animeViewModel: AnimeViewModel by viewModels()
    private lateinit var loading: View
    private lateinit var loadingScrim: View
    private lateinit var touchBlocker: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        loading = findViewById(R.id.loading_overlay)
        loadingScrim = findViewById(R.id.loading_scrim)
        touchBlocker = findViewById(R.id.touch_blocker)

        currentSlug = intent.getStringExtra("currentSlug") ?: ""
        server = intent.getStringExtra("currentServer") ?: ""

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        playerView = findViewById(R.id.player_view)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) showLoading(false)
            }
            override fun onPlayerError(error: PlaybackException) {
                showLoading(false)
            }
        })

        val nextButton: View? = playerView.findViewById(R.id.btn_next_episode)
        nextButton?.isEnabled = true
        nextButton?.alpha = 1f
        nextButton?.setOnClickListener {
            player.pause()
            showLoading(true)
            lifecycleScope.launch {
                val playable = animeViewModel.findPlayableForNext(
                    currentSlug,
                    context = this@VideoPlayerActivity
                )
                if (playable != null) {
                    loadIntoSamePlayer(playable)
                } else {
                    showLoading(false)
                    Toast.makeText(this@VideoPlayerActivity, "No se puede reproducir el siguiente episodio", Toast.LENGTH_LONG).show()
                }
            }
        }


        val videoUrl = intent.getStringExtra("videoUrl")
        val headers = intent.getSerializableExtra("headers") as? HashMap<String, String> ?: hashMapOf()

        if (videoUrl != null) {
            val isHls = videoUrl.contains(".m3u8") || videoUrl.contains("urlset") // puedes afinar este check

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else MimeTypes.APPLICATION_MP4)
                .build()

            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setDefaultRequestProperties(headers)
            }

            val mediaSource = if (isHls) {
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            } else {
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }

            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
        }
    }

    private fun showLoading(show: Boolean) {
        loading.visibility = if (show) View.VISIBLE else View.GONE
        loadingScrim.visibility = if (show) View.VISIBLE else View.GONE
        touchBlocker.visibility = if (show) View.VISIBLE else View.GONE

        // (Opcional) desactivar/activar controles del PlayerView para evitar foco/gestos
        playerView.useController = !show

        // (Opcional) si quieres “apagar” visualmente el botón Next:
        playerView.findViewById<View>(R.id.btn_next_episode)?.apply {
            isEnabled = !show
            alpha = if (show) 0.5f else 1f
        }
    }

    private fun loadIntoSamePlayer(p: AnimeViewModel.Playable) {
        loadUrlHeaders(p.url, p.headers)
        // Actualiza estado actual
        currentSlug = p.slug
        server = p.server
    }

    private fun loadUrlHeaders(url: String, headers: Map<String, String>) {
        val isHls = url.contains(".m3u8") || url.contains("urlset")

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .setMimeType(if (isHls) MimeTypes.APPLICATION_M3U8 else MimeTypes.APPLICATION_MP4)
            .build()

        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(headers)
        }

        val mediaSource = if (isHls) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        // Reutiliza el MISMO player:
        player.setMediaSource(mediaSource, /* startPositionMs = */ 0)
        player.prepare()
        player.play()
    }


    override fun onStart() {
        super.onStart()
        playerView.player = player
    }

    override fun onStop() {
        super.onStop()
        playerView.player = null
        player.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
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
