package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore


class SoloMainActivity : AppCompatActivity(){

  private lateinit var fStore: FirebaseFirestore

  private lateinit var difficulty: RadioGroup
  private lateinit var error: TextView
  private lateinit var start: Button
  private lateinit var cancel: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_solo_main)

    fStore = FirebaseFirestore.getInstance()

    difficulty = findViewById(R.id.rgDifficulty)
    error = findViewById(R.id.tvError)
    start = findViewById(R.id.btnStart)
    cancel = findViewById(R.id.btnCancel)

    val selectedTheme = intent.getStringExtra("selectedTheme")

    start.setOnClickListener {
      val diffId = difficulty.checkedRadioButtonId
      if (diffId != -1) {
        val selectedDiff = findViewById<RadioButton>(diffId).text.toString().lowercase()

        when (selectedDiff) {
          "medium" -> {
            Toast.makeText(this, "Difficulty not available yet.", Toast.LENGTH_SHORT).show()
            difficulty.clearCheck()
            return@setOnClickListener
          }
          "hard" -> {
            Toast.makeText(this, "Difficulty not available yet.", Toast.LENGTH_SHORT).show()
            difficulty.clearCheck()
            return@setOnClickListener
          }
        }
        //start the game

        //TODO: create ng collection attemptHistory dito isesens yung user_uid, start timestamp, theme, etc.
        val intent = Intent(this, MainGameActivity::class.java)
        intent.putExtra("selectedDifficulty", selectedDiff)
        intent.putExtra("selectedTheme", selectedTheme)
        startActivity(intent)
      } else {
        error.text = "Please choose a difficulty"
      }

    }

    cancel.setOnClickListener {
      finish()
    }

  }
}