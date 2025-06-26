package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditDeleteQuestionActivity : AppCompatActivity() {

  private lateinit var loadingOverlay: LinearLayout
  private lateinit var questionUID: TextView
  private lateinit var editQuestion: EditText
  private lateinit var editOptionA: EditText
  private lateinit var editOptionB: EditText
  private lateinit var editOptionC: EditText
  private lateinit var editOptionD: EditText
  private lateinit var editDifficulty: RadioGroup
  private lateinit var save: Button
  private lateinit var delete: Button
  private lateinit var cancel: Button

  private lateinit var fStore: FirebaseFirestore

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_edit_delete_question)

    fStore = FirebaseFirestore.getInstance()

    //UI
    questionUID = findViewById(R.id.tvQuestionUID)
    editQuestion = findViewById(R.id.etEditQuestion)
    editOptionA = findViewById(R.id.etEditOptionA)
    editOptionB = findViewById(R.id.etEditOptionB)
    editOptionC = findViewById(R.id.etEditOptionC)
    editOptionD = findViewById(R.id.etEditOptionD)
    editDifficulty = findViewById(R.id.rdEditDifficulty)
    save = findViewById(R.id.btnSave)
    delete = findViewById(R.id.btnDelete)
    cancel = findViewById(R.id.btnCancel)
    loadingOverlay = findViewById(R.id.loadingOverlay)

    //Get intent extras
    val questionId = intent.getStringExtra("questionId")
    val questionText = intent.getStringExtra("questionText")
    val options = intent.getSerializableExtra("options") as? HashMap<String, String>
    val difficulty = intent.getStringExtra("difficulty")

    // Set values to UI
    questionUID.text = questionId ?: "No ID"
    editQuestion.setText(questionText ?: "")

    editOptionA.setText(options?.get("a") ?: "")
    editOptionB.setText(options?.get("b") ?: "")
    editOptionC.setText(options?.get("c") ?: "")
    editOptionD.setText(options?.get("d") ?: "")

    // Set difficulty RadioButton
    when (difficulty?.lowercase()) {
      "easy" -> editDifficulty.check(R.id.rbEditEasy)
      "medium" -> editDifficulty.check(R.id.rbEditMedium)
      "hard" -> editDifficulty.check(R.id.rbEditHard)
    }

    // Setup change detection after setting initial values
    val originalQuestion = questionText ?: ""
    val originalOptions = listOf(
      options?.get("a") ?: "",
      options?.get("b") ?: "",
      options?.get("c") ?: "",
      options?.get("d") ?: ""
    )

    val originalDifficulty = difficulty ?: ""
    setUpChangeDetection(originalQuestion, originalOptions, originalDifficulty)

    save.setOnClickListener {
      saveChanges()
    }

    delete.setOnClickListener {
      AlertDialog.Builder(this)
        .setTitle("Delete [${questionId}]")
        .setMessage("Are you sure you want to delete this question?")
        .setPositiveButton("Yes") { _, _ ->
          delete()
        }
        .setNegativeButton("No", null)
        .show()
    }

    cancel.setOnClickListener {
      finish()
    }
  }

  private fun setUpChangeDetection(
    originalQuestion: String,
    originalOptions: List<String>,
    originalDifficulty: String
  ) {
    val changeListener = {
      val currentQuestion = editQuestion.text.toString()
      val currentOptions = listOf(
        editOptionA.text.toString(),
        editOptionB.text.toString(),
        editOptionC.text.toString(),
        editOptionD.text.toString()
      )
      val selectedDifficultyId = editDifficulty.checkedRadioButtonId
      val selectedDifficulty = when (selectedDifficultyId) {
        R.id.rbEditEasy -> "easy"
        R.id.rbEditMedium -> "medium"
        R.id.rbEditHard -> "hard"
        else -> ""
      }

      val changed = currentQuestion != originalQuestion ||
        currentOptions != originalOptions ||
        selectedDifficulty != originalDifficulty

      save.isEnabled = changed
    }

    val fields = listOf(editQuestion, editOptionA, editOptionB, editOptionC, editOptionD)
    for (field in fields) {
      field.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
          changeListener()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
      })
    }

    editDifficulty.setOnCheckedChangeListener { _, _ ->
      changeListener()
    }
  }

  private fun saveChanges() {
    loadingVisible()
    val questionId = intent.getStringExtra("questionId") ?: return

    val updatedQuestion = editQuestion.text.toString().trim()
    val updatedOptions = mapOf(
      "a" to editOptionA.text.toString().trim(),
      "b" to editOptionB.text.toString().trim(),
      "c" to editOptionC.text.toString().trim(),
      "d" to editOptionD.text.toString().trim()
    )

    val selectedDifficultyId = editDifficulty.checkedRadioButtonId
    val updatedDifficulty = when (selectedDifficultyId) {
      R.id.rbEditEasy -> "easy"
      R.id.rbEditMedium -> "medium"
      R.id.rbEditHard -> "hard"
      else -> ""
    }

    val updateData = mapOf(
      "question" to updatedQuestion,
      "options" to updatedOptions,
      "difficulty" to updatedDifficulty
    )

    fStore.collection("questions").document(questionId)
      .update(updateData)
      .addOnSuccessListener {
        loadingGone()
        AlertDialog.Builder(this)
          .setTitle("Updated Successfully")
          .setMessage("Question updated successfully.")
          .setPositiveButton("Nice!") { _, _ ->
            val intent = Intent(this, ViewQuestionsActivity::class.java)
            startActivity(intent)
            finish()
          }
          .show()
      }
      .addOnFailureListener {
        loadingGone()
        AlertDialog.Builder(this)
          .setTitle("Updated unsuccessful")
          .setMessage("Failed to update the question.")
          .setPositiveButton("OK", null)
          .show()
      }
  }

  private fun delete() {
    val id = intent.getStringExtra("questionId") ?: return
    fStore.collection("questions").document(id)
      .delete()
      .addOnSuccessListener {
        AlertDialog.Builder(this)
          .setTitle("Question Deleted")
          .setMessage("The question has been deleted successfully.")
          .setPositiveButton("OK") { _, _ ->
            val intent = Intent(this, ViewQuestionsActivity::class.java)
            startActivity(intent)
            finish()
          }
          .show()
      }
      .addOnFailureListener {
        AlertDialog.Builder(this)
          .setTitle("Error")
          .setMessage("Failed to delete the question. Please try again.")
          .setPositiveButton("OK", null)
          .show()
      }
  }

  private fun loadingGone() {
    loadingOverlay.visibility = View.GONE
  }

  private fun loadingVisible() {
    loadingOverlay.visibility = View.VISIBLE
  }

}