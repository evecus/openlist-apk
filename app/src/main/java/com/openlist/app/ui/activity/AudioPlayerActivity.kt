package com.openlist.app.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.openlist.app.R
import com.openlist.app.databinding.ActivityAudioPlayerBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

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
        binding.toolbar.setNavigationOnClickListener { finish() }

        initPlayer(url)
    }

    private fun initPlayer(url: String) {
        libVLC = LibVLC(this, arrayListOf("--no-video", "--network-caching=3000"))
        mediaPlayer = MediaPlayer(libVLC)

        val media = Media(libVLC, android.net.Uri.parse(url))
        mediaPlayer.media = media
        media.release()

        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPlayPause.setIconResource(R.drawable.ic_pause)
                    val duration = mediaPlayer.length.coerceAtLeast(0)
                    binding.seekBar.max = duration.toInt()
                    binding.tvDuration.text = formatTime(duration)
                    handler.post(progressUpdater)
                }
                MediaPlayer.Event.Paused -> runOnUiThread {
                    binding.btnPlayPause.setIconResource(R.drawable.ic_play)
                    handler.removeCallbacks(progressUpdater)
                    updateSeekBar()
                }
                MediaPlayer.Event.Buffering -> runOnUiThread {
                    binding.progressBar.visibility =
                        if (event.buffering < 100f) View.VISIBLE else View.GONE
                }
                MediaPlayer.Event.EndReached -> runOnUiThread {
                    handler.removeCallbacks(progressUpdater)
                    binding.btnPlayPause.setIconResource(R.drawable.ic_play)
                    binding.seekBar.progress = binding.seekBar.max
                }
            }
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

        binding.progressBar.visibility = View.VISIBLE
        mediaPlayer.play()
    }

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
        return "%d:%02d".format(m, s % 60)
    }

    override fun onDestroy() {
        handler.removeCallbacks(progressUpdater)
        mediaPlayer.release()
        libVLC.release()
        super.onDestroy()
    }
}
