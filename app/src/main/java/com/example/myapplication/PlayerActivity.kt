package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.databinding.ActivityPlayerBinding
import com.example.myapplication.models.CategoryModels
import com.example.myapplication.models.SongModels
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private var isFavorite = false
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            showGif(isPlaying && exoPlayer.playbackState == Player.STATE_READY)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            showGif(exoPlayer.isPlaying && playbackState == Player.STATE_READY)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)

        // Make the activity fullscreen
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setContentView(binding.root)

        // Load and setup GIF
        Glide.with(this)
            .asGif()
            .load(R.drawable.media_playing)
            .into(binding.songGifImageView)

        MyExoplayer.getCurrentSong()?.apply {
            setupPlayerUI(this)
            setupNavigationControls()
            checkIfFavorite(this)
            setupFavoriteButton(this)
        }
    }

    private fun checkIfFavorite(song: SongModels) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("sections")
            .document("favorites_${userId}")
            .get()
            .addOnSuccessListener { document ->
                val category = document.toObject(CategoryModels::class.java)
                isFavorite = category?.songs?.contains(song.id) == true
                updateFavoriteIcon()
            }
    }

    private fun setupFavoriteButton(song: SongModels) {
        binding.favoriteButton.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val favoritesRef = firestore.collection("sections").document("favorites_${userId}")

            if (isFavorite) {
                // Remove from favorites
                favoritesRef.get().addOnSuccessListener { document ->
                    val category = document.toObject(CategoryModels::class.java)
                    val updatedSongs = category?.songs?.filter { it != song.id } ?: listOf()

                    favoritesRef.set(
                        CategoryModels(
                            name = "My Favorites",
                            coverUrl = song.coverUrl,
                            songs = updatedSongs
                        )
                    ).addOnSuccessListener {
                        isFavorite = false
                        updateFavoriteIcon()
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Add to favorites
                favoritesRef.get().addOnSuccessListener { document ->
                    val category = document.toObject(CategoryModels::class.java)
                    val currentSongs = category?.songs ?: listOf()
                    val updatedSongs = currentSongs + song.id

                    favoritesRef.set(
                        CategoryModels(
                            name = "My Favorites",
                            coverUrl = song.coverUrl,
                            songs = updatedSongs
                        )
                    ).addOnSuccessListener {
                        isFavorite = true
                        updateFavoriteIcon()
                        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateFavoriteIcon() {
        binding.favoriteButton.setImageResource(
            if (isFavorite) R.drawable.like else R.drawable.unlike
        )
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayerUI(song: SongModels) {
        binding.songTitleTextView.text = song.title
        binding.songSubtitleTextView.text = song.subtitle

        // Improve image loading with crossfade and placeholder
        Glide.with(binding.songCoverImageView)
            .load(song.coverUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .circleCrop()
            .error(R.drawable.mpplayer)
            .into(binding.songCoverImageView)

        exoPlayer = MyExoplayer.getInstance()!!
        binding.playerView.apply {
            player = exoPlayer
            setShowNextButton(false)
            setShowPreviousButton(false)
            setShowFastForwardButton(false)
            setShowRewindButton(false)
            controllerHideOnTouch = false
            controllerShowTimeoutMs = 0
        }

        exoPlayer.addListener(playerListener)

        // Show GIF if already playing
        showGif(exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_READY)
    }

    private fun setupNavigationControls() {
        binding.nextButton.setOnClickListener { playNextSong() }
        binding.PreviousButton.setOnClickListener { playPreviousSong() }
    }

    private fun showGif(show: Boolean) {
        binding.songGifImageView.apply {
            if (show && visibility != View.VISIBLE) {
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            } else if (!show && visibility == View.VISIBLE) {
                animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { visibility = View.INVISIBLE }
                    .start()
            }
        }
    }

    private fun playPreviousSong() {
        MyExoplayer.currentPlaylist.let { playlist ->
            if (playlist.isEmpty()) {
                showToast("No songs in playlist")
                return
            }

            val currentIndex = playlist.indexOfFirst { it == MyExoplayer.getCurrentSong()?.id }
            val prevIndex = if (currentIndex == 0) playlist.size - 1 else currentIndex - 1
            playSongAtIndex(prevIndex)
        }
    }

    private fun getPreviousSong(currentSong: SongModels, onSuccess: (SongModels) -> Unit, onFailure: () -> Unit) {
        val currentSongIndex = SongsListActivity.category.songs.indexOfFirst { it == currentSong.id }
        val previousSongIndex = if (currentSongIndex == 0) {
            SongsListActivity.category.songs.size - 1
        } else {
            (currentSongIndex - 1) % SongsListActivity.category.songs.size
        }
        val previousSongId = SongsListActivity.category.songs[previousSongIndex]

        FirebaseFirestore.getInstance().collection("songs").document(previousSongId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val previousSong = documentSnapshot.toObject(SongModels::class.java)
                if (previousSong != null) {
                    onSuccess(previousSong)
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                onFailure()
            }
    }

    private fun playNextSong() {
        MyExoplayer.currentPlaylist.let { playlist ->
            if (playlist.isEmpty()) {
                showToast("No songs in playlist")
                return
            }

            val currentIndex = playlist.indexOfFirst { it == MyExoplayer.getCurrentSong()?.id }
            val nextIndex = (currentIndex + 1) % playlist.size
            playSongAtIndex(nextIndex)
        }
    }

    private fun getNextSong(currentSong: SongModels, onSuccess: (SongModels) -> Unit, onFailure: () -> Unit) {
        val currentSongIndex = SongsListActivity.category.songs.indexOfFirst { it == currentSong.id }
        val nextSongIndex = (currentSongIndex + 1) % SongsListActivity.category.songs.size
        val nextSongId = SongsListActivity.category.songs[nextSongIndex]

        FirebaseFirestore.getInstance().collection("songs").document(nextSongId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val nextSong = documentSnapshot.toObject(SongModels::class.java)
                if (nextSong != null) {
                    onSuccess(nextSong)
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                onFailure()
            }
    }

    private fun updateUI(song: SongModels) {
        binding.songTitleTextView.text = song.title
        binding.songSubtitleTextView.text = song.subtitle
        Glide.with(binding.songCoverImageView)
            .load(song.coverUrl)
            .apply(RequestOptions().transform(RoundedCorners(32)))
            .into(binding.songCoverImageView)
    }

    private fun playSongAtIndex(index: Int) {
        val playlist = MyExoplayer.currentPlaylist
        if (index !in playlist.indices) {
            showToast("Invalid song index")
            return
        }

        FirebaseFirestore.getInstance().collection("songs")
            .document(playlist[index])
            .get()
            .addOnSuccessListener { documentSnapshot ->
                documentSnapshot.toObject(SongModels::class.java)?.let { song ->
                    MyExoplayer.startPlaying(
                        context = this,
                        song = song,
                        playlist = playlist,
                        isSearch = MyExoplayer.isSearchContext
                    )
                    updateUI(song)
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.removeListener(playerListener)
    }
}