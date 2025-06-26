package com.example.quizguru

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditDeleteVideoActivity : AppCompatActivity() {

  private lateinit var fStore: FirebaseFirestore

  private lateinit var back: Button
  private lateinit var toVideoQuestion: Button
  private lateinit var videoPreview: WebView
  private lateinit var videoUrl: TextView
  private lateinit var videoTheme: TextView
  private lateinit var videoTimestamp: TextView
  private lateinit var videoTitle: EditText
  private lateinit var videoDescription: EditText
  private lateinit var videoOrder: Spinner
  private lateinit var edit: Button
  private lateinit var save: Button
  private lateinit var cancel: Button
  private lateinit var delete: Button


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_edit_delete_video)

    fStore = FirebaseFirestore.getInstance()

    back = findViewById(R.id.btnBack)
    toVideoQuestion = findViewById(R.id.btnVideoQuestions)
    videoPreview = findViewById(R.id.wvVideoPreview)
    videoUrl = findViewById(R.id.tvVideoUrl)
    videoTheme = findViewById(R.id.tvVideoTheme)
    videoTimestamp = findViewById(R.id.tvTimestamp)
    videoTitle = findViewById(R.id.etEditTitle)
    videoDescription = findViewById(R.id.etEditDescription)
    videoOrder = findViewById(R.id.spEditOrder)
    edit = findViewById(R.id.btnEdit)
    save = findViewById(R.id.btnSave)
    cancel = findViewById(R.id.btnCancel)
    delete = findViewById(R.id.btnDelete)

    val videoTitleStr = intent.getStringExtra("video_title") ?: ""
    val videoDescriptionStr = intent.getStringExtra("video_description") ?: ""
    val videoThemeStr = intent.getStringExtra("video_theme") ?: ""
    val videoOrderValue = intent.getIntExtra("order", -1)
    val videoTimestampStr = intent.getStringExtra("video_timestamp") ?: ""
    onView()
    loadOrder(videoOrderValue)

    // Load to views
    videoTitle.setText(videoTitleStr)
    videoDescription.setText(videoDescriptionStr)
    videoTheme.text = videoThemeStr
    videoTimestamp.text = videoTimestampStr

    val videoUrlwv = intent.getStringExtra("video_url") ?: ""
    val embedUrl = convertToEmbedUrl(videoUrlwv)

    if (embedUrl.isNotEmpty()) {
      videoPreview.settings.javaScriptEnabled = true
      videoPreview.settings.domStorageEnabled = true
      videoPreview.webViewClient = WebViewClient()

      val html = """
          <html>
            <body style="margin:0">
              <iframe width="100%" height="100%" 
                src="$embedUrl"
                frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen>
              </iframe>
            </body>
          </html>
      """.trimIndent()

      videoPreview.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
    } else {
      Toast.makeText(this, "Invalid YouTube URL", Toast.LENGTH_SHORT).show()
    }

    val videoUid = intent.getStringExtra("video_id")

    toVideoQuestion.setOnClickListener {
      val intent = Intent(this, ViewVideoQuestionActivity::class.java)
      intent.putExtra("video_id", videoUid)
      startActivity(intent)
    }

    save.setOnClickListener {
      val updatedTitle = videoTitle.text.toString().trim()
      val updatedDescription = videoDescription.text.toString().trim()
      val newOrder = videoOrder.selectedItem?.toString()?.toIntOrNull() ?: -1

      if (newOrder == -1) {
        Toast.makeText(this, "Please select a valid order", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      val currentVideoId = intent.getStringExtra("video_id") ?: return@setOnClickListener
      val currentOrder = intent.getIntExtra("order", -1)

      if (currentOrder == -1) {
        Toast.makeText(this, "Invalid current order", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }

      val currentVideoRef = fStore.collection("videos").document(currentVideoId)

      if (newOrder == currentOrder) {
        val updates = mapOf(
          "title" to updatedTitle,
          "description" to updatedDescription
        )

        currentVideoRef.update(updates)
          .addOnSuccessListener {
            AlertDialog.Builder(this)
              .setTitle("$updatedTitle updated successfully")
              .setPositiveButton("Nice!", null)
              .show()
            onView()
          }
          .addOnFailureListener {
            Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
          }
      } else {
        fStore.collection("videos")
          .whereEqualTo("order", newOrder)
          .get()
          .addOnSuccessListener { result ->
            if (!result.isEmpty) {
              val otherDoc = result.documents[0]
              val otherVideoId = otherDoc.id

              // Perform both updates
              val batch = fStore.batch()

              // 1. Update this video
              val updatesCurrent = mapOf(
                "title" to updatedTitle,
                "description" to updatedDescription,
                "order" to newOrder
              )
              batch.update(currentVideoRef, updatesCurrent)

              // 2. Update the other video to take old order
              val otherVideoRef = fStore.collection("videos").document(otherVideoId)
              batch.update(otherVideoRef, mapOf("order" to currentOrder))

              batch.commit()
                .addOnSuccessListener {
                  AlertDialog.Builder(this)
                    .setTitle("Video order swapped!")
                    .setMessage("Updated $updatedTitle and swapped order with ${otherDoc.getString("title")}")
                    .setPositiveButton("OK", null)
                    .show()
                  onView()
                }
                .addOnFailureListener {
                  Toast.makeText(this, "Swap failed", Toast.LENGTH_SHORT).show()
                }
            } else {
              // No conflict, just update normally
              val updates = mapOf(
                "title" to updatedTitle,
                "description" to updatedDescription,
                "order" to newOrder
              )

              currentVideoRef.update(updates)
                .addOnSuccessListener {
                  AlertDialog.Builder(this)
                    .setTitle("$updatedTitle updated")
                    .setPositiveButton("OK", null)
                    .show()
                  onView()
                }
                .addOnFailureListener {
                  Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
          }
          .addOnFailureListener {
            Toast.makeText(this, "Order check failed", Toast.LENGTH_SHORT).show()
          }

      }
    }

    edit.setOnClickListener {
      onEdit()
    }

    cancel.setOnClickListener {
      onView()
    }

    back.setOnClickListener {
      val intent = Intent(this, ViewVideosActivity::class.java)
      startActivity(intent)
      finish()
    }

    delete.setOnClickListener {
      val videoId = intent.getStringExtra("video_id") ?: return@setOnClickListener

      AlertDialog.Builder(this)
        .setTitle("Delete Video")
        .setMessage("Are you sure you want to delete this video?")
        .setPositiveButton("Delete") { _, _ ->
          fStore.collection("videos")
            .document(videoId)
            .delete()
            .addOnSuccessListener {
              Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show()
              startActivity(Intent(this, ViewVideosActivity::class.java))
              finish()
            }
            .addOnFailureListener { e ->
              Toast.makeText(this, "Failed to delete video: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
    }


  }

  private fun loadOrder(currentOrder: Int) {
    val orderList = mutableListOf<Int>()

    fStore.collection("videos")
      .get()
      .addOnSuccessListener { result ->
        for (document in result) {
          val order = document.getLong("order")?.toInt()
          order?.let {
            if(!orderList.contains(it))
              orderList.add(it)
          }
        }

        if (!orderList.contains(currentOrder)) {
          orderList.add(0, currentOrder)
        }

        orderList.sort()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, orderList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        videoOrder.adapter = adapter

        val index = orderList.indexOf(currentOrder)
        if(index >= 0) {
          videoOrder.setSelection(index)
        }
      }
  }

  private fun onEdit() {
    delete.visibility = View.GONE
    edit.visibility = View.GONE
    save.visibility = View.VISIBLE
    cancel.visibility = View.VISIBLE

    videoTitle.isEnabled = true
    videoTitle.isFocusable = true
    videoTitle.isFocusableInTouchMode = true

    videoDescription.isEnabled = true
    videoDescription.isFocusable = true
    videoDescription.isFocusableInTouchMode = true

    videoOrder.isEnabled = true
    videoOrder.isClickable = true

    back.isEnabled = false
    toVideoQuestion.isEnabled = false
  }

  private fun onView() {
    delete.visibility = View.VISIBLE
    edit.visibility = View.VISIBLE
    save.visibility = View.GONE
    cancel.visibility = View.GONE

    videoTitle.isEnabled = false
    videoTitle.isFocusable = false
    videoTitle.isFocusableInTouchMode = false

    videoDescription.isEnabled = false
    videoDescription.isFocusable = false
    videoDescription.isFocusableInTouchMode = false

    videoOrder.isEnabled = false
    videoOrder.isClickable = false


    back.isEnabled = true
    toVideoQuestion.isEnabled = true
  }

  private fun convertToEmbedUrl(youtubeUrl: String): String {
    return try {
      val uri = Uri.parse(youtubeUrl)
      val host = uri.host ?: ""


      //handle standar yt links
      if (host.contains("youtube.com")) {
        val videoId = uri.getQueryParameter("v")
        "https://www.youtube.com/embed/$videoId"
      }
      // handle short yt links
      else if (host.contains("youtu.be")) {
        val videoId = uri.lastPathSegment
        "https://www.youtube.com/embed/$videoId"
      } else {
        "" // unsupported format
      }
    } catch (e: Exception) {
      ""
    }
  }

}