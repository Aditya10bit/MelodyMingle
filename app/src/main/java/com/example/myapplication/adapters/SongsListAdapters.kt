package com.example.myapplication.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.MyExoplayer
import com.example.myapplication.PlayerActivity

import com.example.myapplication.databinding.SongListItemRecyclerviewBinding
import com.example.myapplication.models.SongModels
import com.google.firebase.firestore.FirebaseFirestore


class SongsListAdapters (private val songidList: List<String>):
    RecyclerView.Adapter<SongsListAdapters.MyViewHolder>() {

    class MyViewHolder(private val binding:SongListItemRecyclerviewBinding):RecyclerView.ViewHolder(binding.root){
        //bind data with view
        fun bindData(songId : String){
           FirebaseFirestore.getInstance().collection("songs")
               .document(songId).get()
               .addOnSuccessListener {
                   val song= it.toObject(SongModels::class.java)
                   song?.apply {
                       binding.songTitleTextView.text=title
                       binding.songSubtitleTextView.text=subtitle
                       Glide.with(binding.songCoverImageView).load(coverUrl).
                       apply(RequestOptions().transform(RoundedCorners(32)))
                           .into(binding.songCoverImageView)
                        binding.root.setOnClickListener{
                            MyExoplayer.startPlaying(binding.root.context,song)
                            it.context.startActivity(Intent(it.context,PlayerActivity::class.java))
                        }
                   }
               }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = SongListItemRecyclerviewBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return songidList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindData(songidList[position])
    }
}