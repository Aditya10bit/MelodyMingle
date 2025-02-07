package com.example.myapplication


import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import com.example.myapplication.models.SongModels
import com.example.myapplication.services.MusicService
import com.google.firebase.firestore.FirebaseFirestore

object MyExoplayer {
    private var exoPlayer: ExoPlayer? = null
    private var currentSong: SongModels? = null
    var currentPlaylist: List<String> = emptyList()
    var isSearchContext = false

    fun playPause() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer = null
        currentSong = null
    }

    fun getCurrentSong(): SongModels? {
        return currentSong
    }

    fun getInstance(): ExoPlayer? {
        return exoPlayer
    }

    fun playNext() {
        currentSong?.let { current ->
            val currentIndex = currentPlaylist.indexOf(current.id)
            if (currentIndex != -1 && currentIndex < currentPlaylist.size - 1) {
                // Get next song ID
                val nextSongId = currentPlaylist[currentIndex + 1]

                // Fetch song details from Firestore and play
                FirebaseFirestore.getInstance().collection("songs")
                    .document(nextSongId)
                    .get()
                    .addOnSuccessListener { document ->
                        document?.toObject(SongModels::class.java)?.let { nextSong ->
                            exoPlayer?.let { player ->
                                currentSong = nextSong
                                updateCount()
                                nextSong.url?.let { url ->
                                    player.setMediaItem(androidx.media3.common.MediaItem.fromUri(url))
                                    player.prepare()
                                    player.play()
                                }
                            }
                        }
                    }
            }
        }
    }

    fun playPrevious() {
        currentSong?.let { current ->
            val currentIndex = currentPlaylist.indexOf(current.id)
            if (currentIndex > 0) {
                // Get previous song ID
                val previousSongId = currentPlaylist[currentIndex - 1]

                // Fetch song details from Firestore and play
                FirebaseFirestore.getInstance().collection("songs")
                    .document(previousSongId)
                    .get()
                    .addOnSuccessListener { document ->
                        document?.toObject(SongModels::class.java)?.let { previousSong ->
                            exoPlayer?.let { player ->
                                currentSong = previousSong
                                updateCount()
                                previousSong.url?.let { url ->
                                    player.setMediaItem(androidx.media3.common.MediaItem.fromUri(url))
                                    player.prepare()
                                    player.play()
                                }
                            }
                        }
                    }
            }
        }
    }

    fun startPlaying(
        context: Context,
        song: SongModels,
        playlist: List<String> = emptyList(),
        isSearch: Boolean = false
    ) {
        currentPlaylist = playlist
        isSearchContext = isSearch
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }

        if (currentSong != song) {
            currentSong = song
            updateCount()
            currentSong?.url?.apply {
                val serviceIntent = Intent(context, MusicService::class.java)
                context.startService(serviceIntent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                exoPlayer?.setMediaItem(androidx.media3.common.MediaItem.fromUri(this))
                exoPlayer?.prepare()
                exoPlayer?.play()
            }
        }
    }

    fun updateCount() {
        currentSong?.id?.let { id ->
            FirebaseFirestore.getInstance().collection("songs")
                .document(id).get().addOnSuccessListener {
                    var latestcount = it.getLong("count")
                    if (latestcount == null) {
                        latestcount = 1L
                    } else {
                        latestcount = latestcount + 1
                    }
                    FirebaseFirestore.getInstance().collection("songs")
                        .document(id).update(mapOf("count" to latestcount))
                }
        }
    }
}