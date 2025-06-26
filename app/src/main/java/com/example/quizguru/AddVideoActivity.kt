package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddVideoActivity : AppCompatActivity() {

  private lateinit var fAuth: FirebaseAuth
  private lateinit var fStore: FirebaseFirestore

  private lateinit var vTitle: EditText
  private lateinit var vDescription: EditText
  private lateinit var vURL: EditText
  private lateinit var vTheme: Spinner
  private lateinit var addVideo: Button
  private lateinit var clear: Button
  private lateinit var cancel: Button
  private lateinit var error: TextView
  private lateinit var back: Button
  private lateinit var loadingOverlay: LinearLayout

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_add_video)

    fAuth = FirebaseAuth.getInstance()
    fStore = FirebaseFirestore.getInstance()

    vTitle = findViewById(R.id.etVideoTitle)
    vDescription = findViewById(R.id.etVideoDescription)
    vURL = findViewById(R.id.etVideoUrl)
    vTheme = findViewById(R.id.spVideoTheme)
    addVideo = findViewById(R.id.btnAddVideo)
    clear = findViewById(R.id.btnClear)
    cancel = findViewById(R.id.btnCancel)
    error = findViewById(R.id.tvError)
    back = findViewById(R.id.btnBack)
    loadingOverlay = findViewById(R.id.loadingOverlay)

    val userUid = fAuth.currentUser?.uid

    loadThemes()

    addVideo.setOnClickListener {
      val fieldChecker = listOf(vTitle, vDescription, vURL)
      val theme = vTheme.selectedItem.toString()

      if (fieldValidation(fieldChecker, theme)) {

        fStore.collection("videos")
          .orderBy("order", com.google.firebase.firestore.Query.Direction.DESCENDING)
          .limit(1)
          .get()
          .addOnSuccessListener { result ->
            val lastOrder = if (result.documents.isNotEmpty()) {
              result.documents[0].getLong("order")?.toInt() ?: 0
            } else {
              0
            }

            val nextOrder = lastOrder + 1

            val videoData = hashMapOf(
              "title" to vTitle.text.toString().trim(),
              "description" to vDescription.text.toString().trim(),
              "url" to vURL.text.toString().trim(),
              "theme" to theme,
              "order" to nextOrder,
              "created_at" to FieldValue.serverTimestamp(),
              "uploaded_by" to userUid
            )
            loadingOverlay.visibility = LinearLayout.VISIBLE

            fStore.collection("videos")
              .add(videoData)
              .addOnSuccessListener {
                loadingOverlay.visibility = LinearLayout.GONE
                AlertDialog.Builder(this)
                  .setTitle("Success")
                  .setMessage("Added the video successfully.")
                  .setPositiveButton("Nice!") { _, _ -> clear() }
                  .show()
              }
              .addOnFailureListener { e ->
                loadingOverlay.visibility = LinearLayout.GONE
                AlertDialog.Builder(this)
                  .setTitle("Failed")
                  .setMessage("Failed to add video: ${e.message}")
                  .setPositiveButton("OK", null)
                  .show()
              }
          }
      }
    }


    clear.setOnClickListener {
      clear()
    }

    back.setOnClickListener {
      finish()
    }

  }

  private fun loadThemes() {
    val themeList = mutableListOf("-- Select Theme --")

    fStore.collection("themes")
      .get()
      .addOnSuccessListener { result ->
        for (document in result) {
          val name = document.getString("name")
          name?.let {
            themeList.add(it)
          }
        }

        //adapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vTheme.adapter = adapter
      }
      .addOnFailureListener { e ->
        error.text = "Failed to load themes: ${e.message}"
      }
  }

  private fun fieldValidation(
    textField: List<EditText>,
    theme: String
    ): Boolean {
    var valid = true
    error.text = ""

    for (field in textField) {
      if (TextUtils.isEmpty(field.text.toString().trim())) {
        field.error = " This field is required"
        valid = false
      }
    }
    if(theme == "-- Select Theme --") {
      error.text = "Please pick a theme."
      valid = false
    }

    return valid
  }


  private fun clear() {
    vTitle.text.clear()
    vDescription.text.clear()
    vURL.text.clear()
    vTheme.setSelection(0)
    error.text = ""
  }
}