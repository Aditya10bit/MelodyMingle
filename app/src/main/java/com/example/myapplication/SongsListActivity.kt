package com.example.myapplication

import android.os.Bundle
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
    companion object{
       lateinit var category:CategoryModels
    }
    lateinit var binding: ActivitySongsListBinding
    lateinit var songsListAdapters: SongsListAdapters
    override fun onCreate(savedInstanceState: Bundle?) {
        binding=ActivitySongsListBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(binding.root)
        binding.nameTextView.text= category.name
        Glide.with(binding.coverImageView).load(category.coverUrl).
        apply(RequestOptions().transform(RoundedCorners(32)))
            .into(binding.coverImageView)
        setupSongsListRecyclerView()
    }

    fun setupSongsListRecyclerView(){
        songsListAdapters= SongsListAdapters(category.songs)
        binding.songsListRecyclerview.layoutManager=LinearLayoutManager(this)
        binding.songsListRecyclerview.adapter=songsListAdapters
    }
}