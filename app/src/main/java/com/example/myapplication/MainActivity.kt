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
import androidx.core.widget.addTextChangedListener
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
    lateinit var categoryAdapter: CategoryAdapter
    private lateinit var allSongs: List<SongModels>
    private var isSearchVisible = false

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
        setupSection("section_1", binding.section1MainLayout, binding.section1Title, binding.section1Recyclerview)
        setupSection("section_2", binding.section2MainLayout, binding.section2Title, binding.section2Recyclerview)
        setupSection("section_3", binding.section3MainLayout, binding.section3Title, binding.section3Recyclerview)
        setupSection("remixes", binding.remixesMainLayout, binding.remixesTitle, binding.remixesRecyclerview)
        setupMostlyPlayed("mostly_played", binding.mostlyPlayedMainLayout, binding.mostlyPlayedTitle, binding.mostlyPlayedRecyclerview)

        binding.menu.setOnClickListener {
            showPopupMenu()
        }

        // Set up search functionality
        setupSearch()
    }

    private fun setupSearch() {
        binding.search.setOnClickListener {
            toggleSearchVisibility()
        }

        binding.searchEditText.addTextChangedListener { text ->
            if (text.isNullOrEmpty()) {
                resetSearch()
            } else {
                performSearch(text.toString())
            }
        }
    }

    private fun toggleSearchVisibility() {
        isSearchVisible = !isSearchVisible
        binding.searchEditText.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
        if (!isSearchVisible) {
            binding.searchEditText.text.clear()
            resetSearch()
        }
    }

    private fun performSearch(query: String) {
        val filteredSongs = allSongs.filter { song ->
            song.title.contains(query, ignoreCase = true) || song.subtitle.contains(query, ignoreCase = true)
        }

        if (filteredSongs.isEmpty()) {
            // Show "Song not present" message
            binding.categoriesRecyclerview.visibility = View.GONE
            binding.section1MainLayout.visibility = View.GONE
            binding.section2MainLayout.visibility = View.GONE
            binding.section3MainLayout.visibility = View.GONE
            binding.remixesMainLayout.visibility = View.GONE
            binding.mostlyPlayedMainLayout.visibility = View.GONE

            // You might want to add a TextView to show this message
            // binding.noSongsFoundTextView.visibility = View.VISIBLE
            // binding.noSongsFoundTextView.text = "Song not present"
        } else {
            // Update the RecyclerView with filtered songs
            updateRecyclerViewWithFilteredSongs(filteredSongs)
        }
    }

    private fun resetSearch() {
        binding.categoriesRecyclerview.visibility = View.VISIBLE
        binding.section1MainLayout.visibility = View.VISIBLE
        binding.section2MainLayout.visibility = View.VISIBLE
        binding.section3MainLayout.visibility = View.VISIBLE
        binding.remixesMainLayout.visibility = View.VISIBLE
        binding.mostlyPlayedMainLayout.visibility = View.VISIBLE

        // Hide the "Song not present" message if you added it
        // binding.noSongsFoundTextView.visibility = View.GONE

        // Reset to original data
        getCategories()
        setupSection("section_1", binding.section1MainLayout, binding.section1Title, binding.section1Recyclerview)
        setupSection("section_2", binding.section2MainLayout, binding.section2Title, binding.section2Recyclerview)
        setupSection("section_3", binding.section3MainLayout, binding.section3Title, binding.section3Recyclerview)
        setupSection("remixes", binding.remixesMainLayout, binding.remixesTitle, binding.remixesRecyclerview)
        setupMostlyPlayed("mostly_played", binding.mostlyPlayedMainLayout, binding.mostlyPlayedTitle, binding.mostlyPlayedRecyclerview)
    }

    private fun updateRecyclerViewWithFilteredSongs(filteredSongs: List<SongModels>) {
        // Create a new adapter with filtered songs and update the RecyclerView
        val adapter = SectionSongListAdapter(filteredSongs.map { it.id })
        binding.categoriesRecyclerview.adapter = adapter
        binding.categoriesRecyclerview.visibility = View.VISIBLE

        // Hide other sections
        binding.section1MainLayout.visibility = View.GONE
        binding.section2MainLayout.visibility = View.GONE
        binding.section3MainLayout.visibility = View.GONE
        binding.remixesMainLayout.visibility = View.GONE
        binding.mostlyPlayedMainLayout.visibility = View.GONE
    }

    fun showPopupMenu() {
        val popupMenu = PopupMenu(this, binding.menu)
        val inflator = popupMenu.menuInflater
        inflator.inflate(R.menu.option_menu, popupMenu.menu)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.log_out -> {
                    logOut()
                    true
                }
                else -> false
            }
        }
    }

    fun logOut() {
        MyExoplayer.getInstance()?.stop()
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    fun getCategories() {
        FirebaseFirestore.getInstance().collection("category")
            .get().addOnSuccessListener {
                val categoryList = it.toObjects(CategoryModels::class.java)
                setupCategoryRecyclerView(categoryList)
            }

        FirebaseFirestore.getInstance().collection("songs")
            .get().addOnSuccessListener { snapshot ->
                allSongs = snapshot.toObjects(SongModels::class.java)
            }
    }

    fun setupCategoryRecyclerView(categoryList: List<CategoryModels>) {
        categoryAdapter = CategoryAdapter(categoryList)
        binding.categoriesRecyclerview.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecyclerview.adapter = categoryAdapter
    }

    override fun onResume() {
        super.onResume()
        showPlayerView()
    }

    fun showPlayerView() {
        binding.playerView.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        MyExoplayer.getCurrentSong()?.let {
            binding.playerView.visibility = View.VISIBLE
            binding.songTitleTextView.text = "Now Playing: " + it.title
            Glide.with(binding.songCoverImageView).load(it.coverUrl)
                .apply(RequestOptions().transform(RoundedCorners(32)))
                .into(binding.songCoverImageView)
        } ?: run {
            binding.playerView.visibility = View.GONE
        }
    }

    fun setupSection(id: String, mainLayout: RelativeLayout, titleView: TextView, recyclerView: RecyclerView) {
        Firebase.firestore.collection("sections").document(id)
            .get().addOnSuccessListener {
                val section = it.toObject(CategoryModels::class.java)
                section?.apply {
                    mainLayout.visibility = View.VISIBLE
                    titleView.text = name
                    recyclerView.layoutManager =
                        LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                    recyclerView.adapter = SectionSongListAdapter(songs)
                    mainLayout.setOnClickListener {
                        SongsListActivity.category = section
                        startActivity(Intent(this@MainActivity, SongsListActivity::class.java))
                    }
                }
            }
    }

    fun setupMostlyPlayed(id: String, mainLayout: RelativeLayout, titleView: TextView, recyclerView: RecyclerView) {
        Firebase.firestore.collection("sections").document(id)
            .get().addOnSuccessListener {
                FirebaseFirestore.getInstance().collection("songs")
                    .orderBy("count", Query.Direction.DESCENDING).limit(10)
                    .get().addOnSuccessListener { songListSnapshot ->
                        val songsModelList = mutableListOf<SongModels>()
                        for (document in songListSnapshot.documents) {
                            val songModel = document.toObject(SongModels::class.java)
                            songModel?.let {
                                songsModelList.add(it)
                            }
                        }

                        val songIdList = songsModelList.map { it.id }
                        val section = it.toObject(CategoryModels::class.java)
                        section?.apply {
                            section.songs = songIdList
                            mainLayout.visibility = View.VISIBLE
                            titleView.text = name
                            recyclerView.layoutManager =
                                LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                            recyclerView.adapter = SectionSongListAdapter(songs)
                            mainLayout.setOnClickListener {
                                SongsListActivity.category = section
                                startActivity(Intent(this@MainActivity, SongsListActivity::class.java))
                            }
                        }
                    }
            }
    }
}