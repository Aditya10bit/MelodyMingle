package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.adapters.SongsListAdapters
import com.example.myapplication.databinding.ActivitySongsListBinding
import com.example.myapplication.models.CategoryModels


class SongsListActivity : AppCompatActivity() {
    companion object {
        lateinit var category: CategoryModels
    }
    lateinit var binding: ActivitySongsListBinding
    lateinit var songsListAdapters: SongsListAdapters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySongsListBinding.inflate(layoutInflater)

        // Set window flags for full screen and status bar
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.BLACK
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        setContentView(binding.root)

        // Set up UI components
        binding.nameTextView.text = category.name
        Glide.with(binding.coverImageView)
            .load(category.coverUrl)
            .apply(RequestOptions().transform(RoundedCorners(32)))
            .into(binding.coverImageView)

        setupSongsListRecyclerView()
    }

    private fun setupSongsListRecyclerView() {
        songsListAdapters = SongsListAdapters(category.songs)
        binding.songsListRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.songsListRecyclerview.adapter = songsListAdapters
    }
}