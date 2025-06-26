package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ViewVideosActivity : AppCompatActivity() {

  private lateinit var fStore: FirebaseFirestore

  private lateinit var viewVideo: RecyclerView
  private lateinit var videoAdapter: VideoAdapter
  private val videoList = mutableListOf<Video>()
  private lateinit var theme: Spinner
  private lateinit var back: Button
  private lateinit var noVideo: TextView
  private lateinit var loadingOverlay: LinearLayout

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_view_videos)

    fStore = FirebaseFirestore.getInstance()

    viewVideo = findViewById(R.id.rvViewVideos)
    viewVideo.layoutManager = LinearLayoutManager(this)
    videoAdapter = VideoAdapter(videoList) { selectedVideo ->
      val intent = Intent(this, EditDeleteVideoActivity::class.java)
      intent.putExtra("video_id", selectedVideo.id)
      intent.putExtra("video_url", selectedVideo.url)
      intent.putExtra("video_title", selectedVideo.title)
      intent.putExtra("video_description", selectedVideo.description)
      intent.putExtra("video_theme", selectedVideo.theme)
      intent.putExtra("order", selectedVideo.order)
      intent.putExtra("video_timestamp", selectedVideo.created_at?.toDate().toString())
      startActivity(intent)
      finish()
    }
    viewVideo.adapter = videoAdapter
    theme = findViewById(R.id.spVideoTheme)
    back = findViewById(R.id.btnBack)
    noVideo = findViewById(R.id.tvNoVideos)
    loadingOverlay = findViewById(R.id.loadingOverlay)

    setupTheme()

    theme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedTheme = parent?.getItemAtPosition(position).toString()
        if (selectedTheme == "All") {
          fetchVideos()
        } else {
          fetchVideosByTheme(selectedTheme)
        }
      }

      override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    back.setOnClickListener {
      finish()
    }
  }

  private fun setupTheme() {
    val themeList = mutableListOf<String>()
    themeList.add("All")

    fStore.collection("themes")
      .get()
      .addOnSuccessListener { result ->
        for (doc in result) {
          val themeName = doc.getString("name")
          if (themeName != null) {
            themeList.add(themeName)
          }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        theme.adapter = adapter
      }
      .addOnFailureListener {
        Toast.makeText(this, "Failed to load themes.", Toast.LENGTH_SHORT).show()
      }
  }

  private fun fetchVideos() {
    loadingVisible()
    fStore.collection("videos")
      .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
      .get()
      .addOnSuccessListener { documents ->
        loadingGone()
        videoList.clear()
        for(doc in documents) {
          val video = doc.toObject(Video::class.java)?.copy(id = doc.id)
          if (video != null) {
            videoList.add(video)
          }
        }
        videoAdapter.notifyDataSetChanged()
        if (videoList.isEmpty()) {
          noVideo.visibility = View.VISIBLE
        }
      }
      .addOnFailureListener {
        loadingGone()
        Toast.makeText(this, "Failed to load videos.", Toast.LENGTH_SHORT).show()
      }
  }

  private fun fetchVideosByTheme(selectedTheme: String) {
    loadingVisible()
    fStore.collection("videos")
      .whereEqualTo("theme", selectedTheme)
      .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
      .get()
      .addOnSuccessListener { documents ->
        loadingGone()
        videoList.clear()
        for (doc in documents) {
          val video = doc.toObject(Video::class.java)?.copy(id = doc.id)
          if (video != null) {
            videoList.add(video)
          }
        }

        videoAdapter.notifyDataSetChanged()
        if (videoList.isEmpty()) {
          noVideo.visibility = View.VISIBLE
        }
      }
      .addOnFailureListener { e ->
        loadingGone()
        Toast.makeText(this, "Failed to load videos.", Toast.LENGTH_SHORT).show()
        Log.e("DEBUG", "Error fetching videos", e)
      }
  }

  private fun loadingVisible() {
    loadingOverlay.visibility = View.VISIBLE
  }

  private fun loadingGone() {
    loadingOverlay.visibility = View.GONE
  }
}