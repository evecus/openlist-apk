package com.openlist.app.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.openlist.app.databinding.ActivityAudioPlayerBinding

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioPlayerBinding
    private var player: ExoPlayer? = null

    // 用于定时刷新进度条
    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            updateSeekBar()
            handler.postDelayed(this, 500)
        }
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_THUMB = "extra_thumb"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Unknown"

        binding.tvTitle.text = title

        // 直接用 toolbar 的返回按钮，不调用 setSupportActionBar（NoActionBar主题下会冲突）
        binding.toolbar.setNavigationOnClickListener { finish() }

        initPlayer(url, title)
    }

    private fun initPlayer(url: String, title: String) {
        player = ExoPlayer.Builder(this).build().also { exo ->

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .build()
                )
                .build()

            exo.setMediaItem(mediaItem)
            exo.prepare()
            exo.playWhenReady = true

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    binding.progressBar.visibility =
                        if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE

                    // 播放就绪时初始化 SeekBar 最大值
                    if (state == Player.STATE_READY) {
                        val duration = exo.duration.coerceAtLeast(0L)
                        binding.seekBar.max = duration.toInt()
                        binding.tvDuration.text = formatTime(duration)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    binding.btnPlayPause.setIconResource(
                        if (isPlaying) com.openlist.app.R.drawable.ic_pause
                        else com.openlist.app.R.drawable.ic_play
                    )
                    // 播放时启动进度刷新，暂停时停止
                    if (isPlaying) {
                        handler.post(progressUpdater)
                    } else {
                        handler.removeCallbacks(progressUpdater)
                        updateSeekBar() // 暂停时刷新一次确保位置准确
                    }
                }
            })
        }

        binding.btnPlayPause.setOnClickListener {
            player?.let { if (it.isPlaying) it.pause() else it.play() }
        }

        binding.btnRewind.setOnClickListener {
            player?.seekBack()
        }

        binding.btnFastForward.setOnClickListener {
            player?.seekForward()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 拖动时暂停自动刷新，避免跳动
                handler.removeCallbacks(progressUpdater)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                player?.seekTo(seekBar.progress.toLong())
                // 拖动结束后恢复刷新
                if (player?.isPlaying == true) {
                    handler.post(progressUpdater)
                }
            }
        })
    }

    private fun updateSeekBar() {
        val exo = player ?: return
        val position = exo.currentPosition
        val duration = exo.duration.coerceAtLeast(0L)
        binding.seekBar.max = duration.toInt()
        binding.seekBar.progress = position.toInt()
        binding.tvCurrentTime.text = formatTime(position)
        binding.tvDuration.text = formatTime(duration)
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    override fun onDestroy() {
        handler.removeCallbacks(progressUpdater)
        player?.release()
        player = null
        super.onDestroy()
    }
}
