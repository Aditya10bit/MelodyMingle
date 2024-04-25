package com.example.myapplication


import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.ExoPlayer
import com.example.myapplication.models.SongModels
import com.google.firebase.firestore.FirebaseFirestore

object MyExoplayer {
    private var exoPlayer : ExoPlayer?=null
    private var currentSong : SongModels?=null

    fun getCurrentSong(): SongModels?{
        return  currentSong
    }

    fun getInstance() :ExoPlayer?{
        return exoPlayer
    }

    fun startPlaying(context: Context,song : SongModels) {
        if(exoPlayer==null)
            exoPlayer=ExoPlayer.Builder(context).build()

        if(currentSong!=song){
            //new song
            currentSong=song
            updateCount()
            currentSong?.url?.apply {
                val mediaItem = androidx.media3.common.MediaItem.fromUri(this)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()

            }
        }

    }

    fun updateCount(){
        currentSong?.id?.let {id->
            FirebaseFirestore.getInstance().collection("songs")
                .document(id).get().addOnSuccessListener {
                    var latestcount=it.getLong("count")
                    if(latestcount==null){
                        latestcount =1L
                    }else{
                        latestcount=latestcount+1
                    }
                    FirebaseFirestore.getInstance().collection("songs")
                        .document(id).update(mapOf("count" to latestcount))
                }
        }
    }
}
