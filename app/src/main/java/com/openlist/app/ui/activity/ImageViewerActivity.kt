package com.openlist.app.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.openlist.app.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        Glide.with(this)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(com.openlist.app.R.drawable.ic_image_placeholder)
            .error(com.openlist.app.R.drawable.ic_broken_image)
            .into(binding.photoView)

        binding.progressBar.visibility = View.GONE
    }
}
