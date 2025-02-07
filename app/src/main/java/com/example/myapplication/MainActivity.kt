package com.example.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
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
import com.example.myapplication.services.MusicService
import com.google.android.material.snackbar.Snackbar
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


        setupFavorites(
            "favorites_${FirebaseAuth.getInstance().currentUser?.uid}",
            binding.favoritesMainLayout,
            binding.favoritesTitle,
            binding.favoritesRecyclerview
        )
        // Check if user is logged in
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        requestNotificationPermission()

        // Start the music service
        Intent(this, MusicService::class.java).also { intent ->
            startService(intent)
        }
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.BLACK
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        setContentView(binding.root)
        binding.searchEditText.apply {
            background = ColorDrawable(Color.parseColor("#FFFFFFFF"))
            elevation = 4f
            setPadding(32, 16, 32, 16)
        }
        binding.playerView.apply {
            elevation = 8f
            background = ColorDrawable(Color.parseColor("#1A1A1A"))
            setBackgroundResource(R.drawable.player_background)
        }
        binding.categoriesRecyclerview.layoutAnimation =
            LayoutAnimationController(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))

        getCategories()
        setupSection(
            "section_1",
            binding.section1MainLayout,
            binding.section1Title,
            binding.section1Recyclerview
        )
        setupSection(
            "section_2",
            binding.section2MainLayout,
            binding.section2Title,
            binding.section2Recyclerview
        )
        setupSection(
            "section_3",
            binding.section3MainLayout,
            binding.section3Title,
            binding.section3Recyclerview
        )
        setupSection(
            "remixes",
            binding.remixesMainLayout,
            binding.remixesTitle,
            binding.remixesRecyclerview
        )
        setupMostlyPlayed(
            "mostly_played",
            binding.mostlyPlayedMainLayout,
            binding.mostlyPlayedTitle,
            binding.mostlyPlayedRecyclerview
        )

        binding.menu.setOnClickListener {
            showPopupMenu()
        }

        binding.search.setOnClickListener {
            toggleSearchVisibility()
        }

        binding.playPauseButton.setOnClickListener {
            MyExoplayer.playPause()
            updatePlayPauseButton()
        }


        // Set up search functionality
        setupSearch()

    }

    private fun setupFavorites(
        id: String,
        mainLayout: RelativeLayout,
        titleView: TextView,
        recyclerView: RecyclerView
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Firebase.firestore.collection("sections").document("favorites_${userId}")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val section = snapshot?.toObject(CategoryModels::class.java)
                section?.apply {
                    if (songs.isEmpty()) {
                        mainLayout.visibility = View.GONE
                    } else {
                        mainLayout.visibility = View.VISIBLE
                        titleView.text = name
                        recyclerView.layoutManager =
                            LinearLayoutManager(
                                this@MainActivity,
                                LinearLayoutManager.HORIZONTAL,
                                false
                            )

                        val adapter = SectionSongListAdapter(
                            songIdList = songs,
                            context = this@MainActivity,
                            onSwiped = { songId ->
                                removeSongFromFavorites(songId) {
                                    // Show snackbar with undo action
                                    Snackbar.make(
                                        binding.root,
                                        "Song removed from favorites",
                                        Snackbar.LENGTH_LONG
                                    ).setAction("UNDO") {
                                        addSongToFavorites(songId)
                                    }.show()

                                    // Optional: Refresh the favorites list
                                    setupFavorites(id, mainLayout, titleView, recyclerView)
                                }
                            }
                        )

                        recyclerView.adapter = adapter
                        adapter.attachSwipeHelper(recyclerView)

                        setupRecyclerViewAnimations(recyclerView)

                        mainLayout.setOnClickListener {
                            SongsListActivity.category = section
                            startActivity(Intent(this@MainActivity, SongsListActivity::class.java))
                        }
                    }
                }
            }
    }

    private fun removeSongFromFavorites(songId: String, onComplete: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val favoritesRef = Firebase.firestore.collection("sections")
            .document("favorites_${userId}")

        favoritesRef.get().addOnSuccessListener { document ->
            val category = document.toObject(CategoryModels::class.java)
            val updatedSongs = category?.songs?.filter { it != songId } ?: listOf()

            favoritesRef.update("songs", updatedSongs)
                .addOnSuccessListener { onComplete() }
        }
    }

    private fun addSongToFavorites(songId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val favoritesRef = Firebase.firestore.collection("sections")
            .document("favorites_${userId}")

        favoritesRef.get().addOnSuccessListener { document ->
            val category = document.toObject(CategoryModels::class.java)
            val updatedSongs = category?.songs?.plus(songId) ?: listOf(songId)

            favoritesRef.update("songs", updatedSongs)
        }
    }

    private fun updatePlayPauseButton() {
        binding.playPauseButton.setImageResource(
            if (MyExoplayer.isPlaying()) R.drawable.ic_play_pause
            else R.drawable.ic_play_play
        )
    }



    private fun setupRecyclerViewAnimations(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItems = layoutManager.childCount
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

                for (i in 0 until visibleItems) {
                    val view = layoutManager.getChildAt(i) ?: continue
                    val position = firstVisiblePosition + i

                    // Calculate scale based on position
                    val scale = when {
                        position == firstVisiblePosition -> 0.85f
                        position == firstVisiblePosition + 1 -> 1f
                        position == firstVisiblePosition + 2 -> 0.85f
                        else -> 0.85f
                    }

                    view.scaleX = scale
                    view.scaleY = scale
                }
            }
        })
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
        binding.searchEditText.animate()
            .alpha(if (isSearchVisible) 1f else 0f)
            .setDuration(200)
            .withEndAction {
                binding.searchEditText.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
            }
            .start()

        if (!isSearchVisible) {
            binding.searchEditText.text.clear()
            resetSearch()
        }
    }

    private fun performSearch(query: String) {
        val filteredSongs = allSongs.filter { song ->
            song.title.contains(query, ignoreCase = true) || song.subtitle.contains(
                query,
                ignoreCase = true
            )
        }

        if (filteredSongs.isEmpty()) {
            // Show "Song not present" message
            binding.categoriesRecyclerview.visibility = View.GONE
            binding.section1MainLayout.visibility = View.GONE
            binding.section2MainLayout.visibility = View.GONE
            binding.section3MainLayout.visibility = View.GONE
            binding.remixesMainLayout.visibility = View.GONE
            binding.mostlyPlayedMainLayout.visibility = View.GONE


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



        // Reset to original data
        getCategories()
        setupSection(
            "section_1",
            binding.section1MainLayout,
            binding.section1Title,
            binding.section1Recyclerview
        )
        setupSection(
            "section_2",
            binding.section2MainLayout,
            binding.section2Title,
            binding.section2Recyclerview
        )
        setupSection(
            "section_3",
            binding.section3MainLayout,
            binding.section3Title,
            binding.section3Recyclerview
        )
        setupSection(
            "remixes",
            binding.remixesMainLayout,
            binding.remixesTitle,
            binding.remixesRecyclerview
        )
        setupMostlyPlayed(
            "mostly_played",
            binding.mostlyPlayedMainLayout,
            binding.mostlyPlayedTitle,
            binding.mostlyPlayedRecyclerview
        )
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }

    private fun updateRecyclerViewWithFilteredSongs(filteredSongs: List<SongModels>) {
        val adapter = SectionSongListAdapter(
            songIdList = filteredSongs.map { it.id },
            isSearch = true,
            context = this,
        )
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
        setupRecyclerViewAnimations(binding.categoriesRecyclerview)
    }

    override fun onResume() {
        super.onResume()
        showPlayerView()
        updatePlayPauseButton()
    }
    override fun onDestroy() {
        super.onDestroy()
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
            updatePlayPauseButton()  // Add this line
        } ?: run {
            binding.playerView.visibility = View.GONE
        }
    }

    fun setupSection(
        id: String,
        mainLayout: RelativeLayout,
        titleView: TextView,
        recyclerView: RecyclerView
    ) {
        Firebase.firestore.collection("sections").document(id)
            .get().addOnSuccessListener {
                val section = it.toObject(CategoryModels::class.java)
                section?.apply {
                    mainLayout.visibility = View.VISIBLE
                    titleView.text = name
                    recyclerView.layoutManager =
                        LinearLayoutManager(
                            this@MainActivity,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                    recyclerView.adapter = SectionSongListAdapter(
                        songs,
                        context = this@MainActivity
                    )
                    // Add animation here
                    setupRecyclerViewAnimations(recyclerView)  // Add this line
                    mainLayout.setOnClickListener {
                        SongsListActivity.category = section
                        startActivity(Intent(this@MainActivity, SongsListActivity::class.java))
                    }
                }
            }
    }

    fun setupMostlyPlayed(
        id: String,
        mainLayout: RelativeLayout,
        titleView: TextView,
        recyclerView: RecyclerView
    ) {
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
                                LinearLayoutManager(
                                    this@MainActivity,
                                    LinearLayoutManager.HORIZONTAL,
                                    false
                                )
                            recyclerView.adapter = SectionSongListAdapter(
                                songs,
                                context = this@MainActivity
                            )
                            // Add animation here
                            setupRecyclerViewAnimations(recyclerView)  // Add this line
                            mainLayout.setOnClickListener {
                                SongsListActivity.category = section
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        SongsListActivity::class.java
                                    )
                                )
                            }
                        }
                    }
            }
    }


}