package com.openlist.app.ui.activity

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.openlist.app.R
import com.openlist.app.databinding.ActivityVideoPlayerBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout

class VideoPlayerActivity : AppCompatActivity(),
    IVLCVout.Callback,
    IVLCVout.OnNewVideoLayoutListener {

    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    private var videoVisibleWidth = 0
    private var videoVisibleHeight = 0
    private var videoSarNum = 1
    private var videoSarDen = 1

    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            updateSeekBar()
            handler.postDelayed(this, 500)
        }
    }
    private val hideControlsRunnable = Runnable { hideControls() }
    private var controlsVisible = false

    private var scaleModeIndex = 0
    private val scaleModes = listOf(
        MediaPlayer.ScaleType.SURFACE_BEST_FIT,
        MediaPlayer.ScaleType.SURFACE_FILL,
        MediaPlayer.ScaleType.SURFACE_16_9
    )

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        binding.tvTitle.text = title
        binding.btnBack.setOnClickListener { finish() }
        binding.btnResizeMode.setOnClickListener {
            scaleModeIndex = (scaleModeIndex + 1) % scaleModes.size
            mediaPlayer.videoScale = scaleModes[scaleModeIndex]
        }
        binding.btnPlayPause.setOnClickListener {
            if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
        }
        binding.btnRewind.setOnClickListener {
            mediaPlayer.time = (mediaPlayer.time - 10_000).coerceAtLeast(0)
        }
        binding.btnFastForward.setOnClickListener {
            mediaPlayer.time = (mediaPlayer.time + 10_000).coerceAtMost(mediaPlayer.length)
        }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvCurrentTime.text = formatTime(progress.toLong())
            }
            override fun onStartTrackingTouch(sb: SeekBar) {
                handler.removeCallbacks(progressUpdater)
            }
            override fun onStopTrackingTouch(sb: SeekBar) {
                mediaPlayer.time = sb.progress.toLong()
                if (mediaPlayer.isPlaying) handler.post(progressUpdater)
            }
        })
        binding.surfaceView.setOnClickListener { toggleControls() }

        initVLC(url)
    }

    private fun initVLC(url: String) {
        libVLC = LibVLC(this, arrayListOf("--network-caching=3000", "--no-osd"))
        mediaPlayer = MediaPlayer(libVLC)

        val vout = mediaPlayer.vlcVout
        vout.setVideoView(binding.surfaceView)
        vout.addCallback(this)                      // IVLCVout.Callback
        vout.addOnNewVideoLayoutListener(this)      // IVLCVout.OnNewVideoLayoutListener
        vout.attachViews()

        val media = Media(libVLC, android.net.Uri.parse(url)).apply {
            addOption(":http-reconnect")
        }
        mediaPlayer.media = media
        mediaPlayer.play()
        media.release()

        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                    val duration = mediaPlayer.length.coerceAtLeast(0)
                    binding.seekBar.max = duration.toInt()
                    binding.tvDuration.text = formatTime(duration)
                    handler.post(progressUpdater)
                    showControls()
                }
                MediaPlayer.Event.Paused -> runOnUiThread {
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                    handler.removeCallbacks(progressUpdater)
                    updateSeekBar()
                    showControls()
                }
                MediaPlayer.Event.Buffering -> runOnUiThread {
                    binding.progressBar.visibility =
                        if (event.buffering < 100f) View.VISIBLE else View.GONE
                }
                MediaPlayer.Event.EncounteredError -> runOnUiThread {
                    Toast.makeText(this, "播放失败：格式不支持或网络错误", Toast.LENGTH_LONG).show()
                }
                MediaPlayer.Event.EndReached -> runOnUiThread { finish() }
            }
        }

        binding.progressBar.visibility = View.VISIBLE
    }

    // IVLCVout.Callback — 必须实现，但不需要处理
    override fun onSurfacesCreated(vout: IVLCVout) {}
    override fun onSurfacesDestroyed(vout: IVLCVout) {}

    // IVLCVout.OnNewVideoLayoutListener — 视频尺寸就绪时动态调整 SurfaceView
    override fun onNewVideoLayout(
        vout: IVLCVout,
        width: Int, height: Int,
        visibleWidth: Int, visibleHeight: Int,
        sarNum: Int, sarDen: Int
    ) {
        if (width == 0 || height == 0) return
        videoVisibleWidth = visibleWidth
        videoVisibleHeight = visibleHeight
        videoSarNum = if (sarNum == 0) 1 else sarNum
        videoSarDen = if (sarDen == 0) 1 else sarDen
        handler.post { updateSurfaceSize() }
    }

    private fun updateSurfaceSize() {
        val container = binding.surfaceView.parent as? View ?: return
        val containerW = container.width
        val containerH = container.height
        if (containerW == 0 || containerH == 0 || videoVisibleWidth == 0 || videoVisibleHeight == 0) return

        val videoW = videoVisibleWidth.toFloat() * videoSarNum / videoSarDen
        val videoH = videoVisibleHeight.toFloat()
        val videoAspect = videoW / videoH
        val containerAspect = containerW.toFloat() / containerH

        val surfaceW: Int
        val surfaceH: Int
        if (videoAspect > containerAspect) {
            surfaceW = containerW
            surfaceH = (containerW / videoAspect).toInt()
        } else {
            surfaceH = containerH
            surfaceW = (containerH * videoAspect).toInt()
        }

        val lp = binding.surfaceView.layoutParams
        lp.width = surfaceW
        lp.height = surfaceH
        binding.surfaceView.layoutParams = lp
    }

    // --- 控制栏 ---

    private fun showControls() {
        controlsVisible = true
        binding.topBar.visibility = View.VISIBLE
        binding.bottomBar.visibility = View.VISIBLE
        scheduleHideControls()
    }

    private fun hideControls() {
        controlsVisible = false
        binding.topBar.visibility = View.GONE
        binding.bottomBar.visibility = View.GONE
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3000)
    }

    // --- 进度 ---

    private fun updateSeekBar() {
        val pos = mediaPlayer.time.coerceAtLeast(0)
        val dur = mediaPlayer.length.coerceAtLeast(0)
        binding.seekBar.max = dur.toInt()
        binding.seekBar.progress = pos.toInt()
        binding.tvCurrentTime.text = formatTime(pos)
        binding.tvDuration.text = formatTime(dur)
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val s = ms / 1000
        val m = s / 60
        val h = m / 60
        return if (h > 0) "%d:%02d:%02d".format(h, m % 60, s % 60)
        else "%d:%02d".format(m, s % 60)
    }

    // --- 系统 UI ---

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

    // --- 生命周期 ---

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if (!mediaPlayer.isPlaying) mediaPlayer.play()
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) mediaPlayer.pause()
        handler.removeCallbacks(progressUpdater)
        handler.removeCallbacks(hideControlsRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressUpdater)
        handler.removeCallbacks(hideControlsRunnable)
        val vout = mediaPlayer.vlcVout
        vout.removeCallback(this)
        vout.removeOnNewVideoLayoutListener(this)
        vout.detachViews()
        mediaPlayer.release()
        libVLC.release()
    }
}
