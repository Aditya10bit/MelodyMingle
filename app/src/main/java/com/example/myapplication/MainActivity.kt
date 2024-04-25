package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.myapplication.adapters.CategoryAdapter
import com.example.myapplication.adapters.SectionSongListAdapter
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.models.CategoryModels
import com.example.myapplication.models.SongModels
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore



class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var categoryapapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(binding.root)
        getCategories()
        setupSection("section_1",binding.section1MainLayout,binding.section1Title,binding.section1Recyclerview)
        setupSection("section_2",binding.section2MainLayout,binding.section2Title,binding.section2Recyclerview)
        setupSection("section_3",binding.section3MainLayout,binding.section3Title,binding.section3Recyclerview)
        setupSection("remixes",binding.remixesMainLayout,binding.remixesTitle,binding.remixesRecyclerview)
        setupMostlyPlayed("mostly_played",binding.mostlyPlayedMainLayout,binding.mostlyPlayedTitle,binding.mostlyPlayedRecyclerview)

        binding.menu.setOnClickListener{
            showPopupMenu()
        }


    }

    fun showPopupMenu(){

        val popupMenu=PopupMenu(this,binding.menu)
        val inflator = popupMenu.menuInflater
        inflator.inflate(R.menu.option_menu,popupMenu.menu)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.log_out ->{
                    logOut()
                    true
                }
            }
            false
        }
    }

    fun logOut(){
        MyExoplayer.getInstance()?.release()
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this,LoginActivity::class.java))
        finish()
    }

    //categories
    fun getCategories() {
        FirebaseFirestore.getInstance().collection("category")
            .get().addOnSuccessListener {
                val categoryList = it.toObjects(CategoryModels::class.java)
                setupCategoryRecyclerView(categoryList)
            }
    }

    fun setupCategoryRecyclerView(categoryList: List<CategoryModels>) {
        categoryapapter = CategoryAdapter(categoryList)
        binding.categoriesRecyclerview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecyclerview.adapter = categoryapapter
    }

    override fun onResume() {
        super.onResume()
        showPlayerView()
    }

    fun showPlayerView(){
        binding.playerView.setOnClickListener{
            startActivity(Intent(this,PlayerActivity::class.java))
        }
        MyExoplayer.getCurrentSong()?.let{
            binding.playerView.visibility=View.VISIBLE
            binding.songTitleTextView.text="Now Playing : " +it.title
            Glide.with(binding.songCoverImageView).load(it.coverUrl).
            apply(RequestOptions().transform(RoundedCorners(32)))
                .into(binding.songCoverImageView)
        }?: run {
            binding.playerView.visibility=View.GONE
        }
    }

    //sections

    fun setupSection(id : String , mainLayout : RelativeLayout, titleView : TextView, recyclerView : RecyclerView) {
        Firebase.firestore.collection("sections").document(id)
            .get().addOnSuccessListener {
            val section = it.toObject(CategoryModels::class.java)
                section?.apply {
                    mainLayout.visibility=View.VISIBLE
                    titleView .text=name
                    recyclerView.layoutManager=
                        LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                    recyclerView.adapter=SectionSongListAdapter(songs)
                    mainLayout.setOnClickListener {
                        SongsListActivity.category=section
                       startActivity(Intent(this@MainActivity,SongsListActivity::class.java) )
                    }
                }
            }
    }


    fun setupMostlyPlayed(id : String , mainLayout : RelativeLayout, titleView : TextView, recyclerView : RecyclerView) {
        Firebase.firestore.collection("sections").document(id)
            .get().addOnSuccessListener {
                //getmostly played songs

                FirebaseFirestore.getInstance().collection("songs")
                    .orderBy("count", Query.Direction.DESCENDING).limit(5)
                    .get().addOnSuccessListener {songListSnapshot->
                        val songsModelList = mutableListOf<SongModels>()
                        for (document in songListSnapshot.documents) {
                            val songModel = document.toObject(SongModels::class.java)
                            songModel?.let {
                                songsModelList.add(it)
                            }
                        }

                        val songIdList = songsModelList.map{
                           it.id
                        }.toList()
                        val section = it.toObject(CategoryModels::class.java)
                        section?.apply {
                            section.songs=songIdList
                            mainLayout.visibility=View.VISIBLE
                            titleView .text=name
                            recyclerView.layoutManager=
                                LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                            recyclerView.adapter=SectionSongListAdapter(songs)
                            mainLayout.setOnClickListener {
                                SongsListActivity.category=section
                                startActivity(Intent(this@MainActivity,SongsListActivity::class.java) )
                            }
                        }
                    }

            }
    }






}