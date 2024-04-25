package com.example.myapplication.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.RoundedCorner
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.SongsListActivity
import com.example.myapplication.databinding.CategoryItemRecyclerRowBinding
import com.example.myapplication.models.CategoryModels

class CategoryAdapter (private val categoryList: List<CategoryModels>) :
    RecyclerView.Adapter<CategoryAdapter.MyViewHolder>() {

    class MyViewHolder(private val binding : CategoryItemRecyclerRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindData(category: CategoryModels) {
            binding.nameTextView.text=category.name
            Glide.with(binding.coverImageView).load(category.coverUrl).
            apply(RequestOptions().transform(RoundedCorners(32)))
                .into(binding.coverImageView)

            //startSongsListActivity
            val context = binding.root.context
            binding.root.setOnClickListener {
                SongsListActivity.category=category
                context.startActivity(Intent(context,SongsListActivity::class.java) )
            }
        }
    }

    fun bindData(category : CategoryModels){

        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
       val binding = CategoryItemRecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindData(categoryList[position])
    }

}