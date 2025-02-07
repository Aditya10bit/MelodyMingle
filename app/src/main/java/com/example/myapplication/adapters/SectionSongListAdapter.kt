package com.example.myapplication.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.MyExoplayer
import com.example.myapplication.PlayerActivity
import com.example.myapplication.R
import com.example.myapplication.databinding.SectionSongListRecyclerRowBinding
import com.example.myapplication.models.SongModels
import com.google.firebase.firestore.FirebaseFirestore



class SectionSongListAdapter(
    var songIdList: List<String>,
    private val isSearch: Boolean = false,
    private val context: Context,
    private val onSwiped: ((String) -> Unit)? = null
) : RecyclerView.Adapter<SectionSongListAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: SectionSongListRecyclerRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        //bind data with view
        fun bindData(songId: String) {
            FirebaseFirestore.getInstance().collection("songs")
                .document(songId).get()
                .addOnSuccessListener {
                    val song = it.toObject(SongModels::class.java)
                    song?.apply {
                        binding.songTitleTextView.text = title
                        binding.songSubtitleTextView.text = subtitle
                        Glide.with(binding.songCoverImageView).load(coverUrl)
                            .apply(RequestOptions().transform(RoundedCorners(32)))
                            .into(binding.songCoverImageView)

                        binding.root.setOnClickListener {
                            MyExoplayer.startPlaying(
                                context = binding.root.context,
                                song = song,
                                playlist = songIdList,
                                isSearch = isSearch
                            )
                            it.context.startActivity(Intent(it.context, PlayerActivity::class.java))
                        }
                    }
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = SectionSongListRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return songIdList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindData(songIdList[position])
    }

    private inner class SwipeToDeleteCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        private val background = ColorDrawable(Color.RED)
        private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
        private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
        private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
        private val swipeThreshold = 0.5f

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView

            // Draw red background
            when {
                dX > 0 -> { // Swiping right
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                }
                dX < 0 -> { // Swiping left
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                }
                else -> background.setBounds(0, 0, 0, 0)
            }
            background.draw(c)

            // Calculate icon position
            deleteIcon?.let {
                val deleteIconTop = itemView.top + (itemView.height - intrinsicHeight) / 2
                val deleteIconMargin = (itemView.height - intrinsicHeight) / 2
                val deleteIconLeft: Int
                val deleteIconRight: Int

                if (dX > 0) { // Swiping right
                    deleteIconLeft = itemView.left + deleteIconMargin
                    deleteIconRight = deleteIconLeft + intrinsicWidth
                } else { // Swiping left
                    deleteIconRight = itemView.right - deleteIconMargin
                    deleteIconLeft = deleteIconRight - intrinsicWidth
                }

                it.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconTop + intrinsicHeight)
                it.draw(c)
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val songId = songIdList[position]
                onSwiped?.invoke(songId)
            }
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            return swipeThreshold
        }

        override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
            return defaultValue * 5
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeMovementFlags(0, ItemTouchHelper.UP or ItemTouchHelper.DOWN)
        }
    }

    // Function to attach swipe functionality to RecyclerView
    fun attachSwipeHelper(recyclerView: RecyclerView) {
        if (onSwiped != null) {
            val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback())
            itemTouchHelper.attachToRecyclerView(recyclerView)
        }
    }
}