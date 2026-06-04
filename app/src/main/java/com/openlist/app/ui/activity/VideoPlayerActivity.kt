package com.openlist.app.ui.activity

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.openlist.app.databinding.ActivityVideoPlayerBinding
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var ijkPlayer: IjkMediaPlayer? = null
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

        // 加载 IjkPlayer 所需的 so 库
        IjkMediaPlayer.loadLibrariesOnce(null)
        IjkMediaPlayer.native_profileBegin("libijkplayer.so")

        setupUI(title)
        initializePlayer(url)
    }

    private fun setupUI(title: String) {
        hideSystemUI()
        binding.tvTitle.text = title
        binding.btnBack.setOnClickListener { finish() }

        // IjkVideoView 不支持 resize_mode 切换，按钮切换画面缩放比例
        binding.btnResizeMode.setOnClickListener {
            val view = binding.ijkVideoView
            val modes = listOf(
                tv.danmaku.ijk.media.widget.media.IjkVideoView.AR_ASPECT_FIT_PARENT,
                tv.danmaku.ijk.media.widget.media.IjkVideoView.AR_ASPECT_FILL_PARENT,
                tv.danmaku.ijk.media.widget.media.IjkVideoView.AR_ASPECT_WRAP_CONTENT
            )
            val current = modes.indexOf(view.currentAspectRatioIndex).let {
                if (it < 0) 0 else it
            }
            view.setAspectRatio(modes[(current + 1) % modes.size])
        }

        binding.topBar.visibility = View.GONE

        // 点击视频区域切换顶部栏显示/隐藏
        binding.ijkVideoView.setOnClickListener {
            binding.topBar.visibility =
                if (binding.topBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
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
        val videoView = binding.ijkVideoView

        // IjkPlayer 选项：硬解优先，回退软解
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)          // 启用硬解
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1)
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0)             // 使用 AudioTrack
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1)            // 丢帧避免音视频不同步
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)

        // 网络优化
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 10_000_000)    // 10秒超时（微秒）
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", 1_000_000)

        // 缓冲：最大缓冲 15 秒
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 15 * 1024 * 1024)
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 50)
        videoView.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1)

        videoView.setOnPreparedListener { mp ->
            binding.progressBar.visibility = View.GONE
            mp.isLooping = false
            if (playbackPosition > 0) mp.seekTo(playbackPosition)
            if (playWhenReady) mp.start()
        }

        videoView.setOnInfoListener { _, what, _ ->
            when (what) {
                IMediaPlayer.MEDIA_INFO_BUFFERING_START ->
                    binding.progressBar.visibility = View.VISIBLE
                IMediaPlayer.MEDIA_INFO_BUFFERING_END ->
                    binding.progressBar.visibility = View.GONE
                IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START ->
                    binding.progressBar.visibility = View.GONE
            }
            true
        }

        videoView.setOnErrorListener { _, what, extra ->
            val msg = when (what) {
                IMediaPlayer.MEDIA_ERROR_IO -> "网络连接失败，请检查网络"
                IMediaPlayer.MEDIA_ERROR_MALFORMED -> "视频格式不支持或解码失败"
                IMediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "视频格式不支持或解码失败"
                IMediaPlayer.MEDIA_ERROR_TIMED_OUT -> "网络连接超时，请检查网络"
                else -> "播放失败（what=$what extra=$extra）"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            true
        }

        binding.progressBar.visibility = View.VISIBLE
        videoView.setVideoURI(Uri.parse(url))
        videoView.requestFocus()
        videoView.start()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (playWhenReady) binding.ijkVideoView.start()
    }

    override fun onPause() {
        super.onPause()
        binding.ijkVideoView.let {
            playWhenReady = it.isPlaying
            playbackPosition = it.currentPosition.toLong()
            it.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        IjkMediaPlayer.native_profileEnd()
    }

    private fun releasePlayer() {
        binding.ijkVideoView.let {
            playWhenReady = it.isPlaying
            playbackPosition = it.currentPosition.toLong()
            it.stopPlayback()
            it.release(true)
        }
    }
}
