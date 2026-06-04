package com.openlist.app.ui.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
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

        binding.topBar.visibility = View.GONE
        binding.playerView.setControllerVisibilityListener(
            androidx.media3.ui.PlayerView.ControllerVisibilityListener { visibility ->
                binding.topBar.visibility = visibility
            }
        )
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
        if (player != null) return

        // 自定义缓冲区：高码率视频（4K/高比特率）需要更大的缓冲才能稳定播放
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,   // minBufferMs：至少缓冲15秒再开始播放
                120_000,  // maxBufferMs：最多缓冲120秒（应对高码率场景）
                2_500,    // bufferForPlaybackMs：首次播放只需缓冲2.5秒
                5_000     // bufferForPlaybackAfterRebufferMs：卡顿后恢复需缓冲5秒
            )
            .setTargetBufferBytes(
                // 64MB 缓冲上限，支持高码率视频；默认值仅约 15MB
                64 * 1024 * 1024
            )
            .setPrioritizeTimeOverSizeThresholds(true) // 优先按时长缓冲而非大小
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()
            .also { exoPlayer ->
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

                    override fun onPlayerError(error: PlaybackException) {
                        // 将错误原因显示给用户，方便排查（网络超时、格式不支持等）
                        val msg = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                                "网络连接失败，请检查网络"
                            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                            PlaybackException.ERROR_CODE_DECODING_FAILED ->
                                "视频格式不支持或解码失败"
                            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                                "服务器返回错误，无法播放"
                            else -> "播放失败：${error.message}"
                        }
                        Toast.makeText(this@VideoPlayerActivity, msg, Toast.LENGTH_LONG).show()
                    }
                })
            }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        player?.playWhenReady = playWhenReady
    }

    override fun onPause() {
        super.onPause()
        player?.let {
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            it.playWhenReady = false
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
