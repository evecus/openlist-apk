package com.openlist.app.ui.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.openlist.app.databinding.ActivityVideoPlayerBinding

@UnstableApi
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var playbackPosition = 0L

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        setupUI(title)
        initializePlayer(url)
    }

    private fun setupUI(title: String) {
        hideSystemUI()
        binding.tvTitle.text = title
        binding.btnBack.setOnClickListener { finish() }
        binding.btnResizeMode.setOnClickListener {
            val current = binding.playerView.resizeMode
            binding.playerView.resizeMode = when (current) {
                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun initializePlayer(url: String) {
        if (player != null) return  // already initialized
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.seekTo(playbackPosition)
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.prepare()
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    binding.progressBar.visibility =
                        if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        // Resume playback if player exists and was playing
        player?.playWhenReady = playWhenReady
    }

    override fun onPause() {
        super.onPause()
        // Save position and pause — do NOT release here
        player?.let {
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            it.playWhenReady = false  // pause audio immediately
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.let {
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            it.release()
        }
        player = null
    }
}
