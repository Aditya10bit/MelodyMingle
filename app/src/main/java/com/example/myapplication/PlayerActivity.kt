// PlayerActivity.kt

package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.databinding.ActivityPlayerBinding
import com.example.myapplication.models.SongModels
import com.google.firebase.firestore.FirebaseFirestore

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private var playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            showGif(isPlaying)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MyExoplayer.getCurrentSong()?.apply {
            binding.songTitleTextView.text = title
            binding.songSubtitleTextView.text = subtitle
            Glide.with(binding.songCoverImageView).load(coverUrl)
                .circleCrop().into(binding.songCoverImageView)
            Glide.with(binding.songGifImageView).load(R.drawable.media_playing)
                .circleCrop().into(binding.songGifImageView)
            exoPlayer = MyExoplayer.getInstance()!!
            binding.playerView.player = exoPlayer
            binding.playerView.showController()
            exoPlayer.addListener(playerListener)

            // Set click listener for the next button
            binding.nextButton.setOnClickListener {
                playNextSong()
            }
            binding.PreviousButton.setOnClickListener{
                playPreviousSong()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.removeListener(playerListener)
    }

    private fun showGif(show: Boolean) {
        binding.songGifImageView.isVisible = show
    }


    private fun playPreviousSong() {
        MyExoplayer.getCurrentSong()?.let { currentSong ->
            getPreviousSong(currentSong,
                onSuccess = { PreviousSong ->
                    MyExoplayer.startPlaying(this, PreviousSong)
                    updateUI(PreviousSong)
                },
                onFailure = {
                    // Handle failure to retrieve the next song
                    // For example, display a message indicating no next song available
                }
            )
        }
    }

    private fun getPreviousSong(currentSong: SongModels, onSuccess: (SongModels) -> Unit, onFailure: () -> Unit) {
        // Logic to retrieve the next song based on the current song
        val currentSongIndex = SongsListActivity.category.songs.indexOfFirst { it == currentSong.id }
        var PreviousSongIndex = if (currentSongIndex==0){
            SongsListActivity.category.songs.size-1}
        else {
            (currentSongIndex - 1) % SongsListActivity.category.songs.size
        }
        val PreviousSongId = SongsListActivity.category.songs[PreviousSongIndex]
        FirebaseFirestore.getInstance().collection("songs").document(PreviousSongId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val PreviousSong = documentSnapshot.toObject(SongModels::class.java)
                if (PreviousSong != null) {
                    // Successfully retrieved the next song
                    onSuccess(PreviousSong)
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure to retrieve the next song
                exception.printStackTrace()
                onFailure()
            }
    }



    private fun playNextSong() {
        MyExoplayer.getCurrentSong()?.let { currentSong ->
            getNextSong(currentSong,
                onSuccess = { nextSong ->
                    MyExoplayer.startPlaying(this, nextSong)
                    updateUI(nextSong)
                },
                onFailure = {
                    // Handle failure to retrieve the next song
                    // For example, display a message indicating no next song available
                }
            )
        }
    }


    private fun getNextSong(currentSong: SongModels, onSuccess: (SongModels) -> Unit, onFailure: () -> Unit) {
        // Logic to retrieve the next song based on the current song
        val currentSongIndex = SongsListActivity.category.songs.indexOfFirst { it == currentSong.id }
        val nextSongIndex = (currentSongIndex + 1) % SongsListActivity.category.songs.size

        val nextSongId = SongsListActivity.category.songs[nextSongIndex]
        FirebaseFirestore.getInstance().collection("songs").document(nextSongId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val nextSong = documentSnapshot.toObject(SongModels::class.java)
                if (nextSong != null) {
                    // Successfully retrieved the next song
                    onSuccess(nextSong)
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure to retrieve the next song
                exception.printStackTrace()
                onFailure()
            }
    }



    private fun updateUI(song: SongModels) {
        binding.songTitleTextView.text = song.title
        binding.songSubtitleTextView.text = song.subtitle
        Glide.with(binding.songCoverImageView).load(song.coverUrl)
            .apply(RequestOptions().transform(RoundedCorners(32)))
            .into(binding.songCoverImageView)
    }
}