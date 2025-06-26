package com.example.quizguru

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class EditDeleteDrillActivity : AppCompatActivity() {

  private lateinit var fStore: FirebaseFirestore

  private lateinit var etDrillQuestion: EditText
  private lateinit var cbOption1: CheckBox
  private lateinit var etEditOption1: EditText
  private lateinit var cbOption2: CheckBox
  private lateinit var etEditOption2: EditText
  private lateinit var cbOption3: CheckBox
  private lateinit var etEditOption3: EditText
  private lateinit var cbOption4: CheckBox
  private lateinit var etEditOption4: EditText
  private lateinit var cbOption5: CheckBox
  private lateinit var etEditOption5: EditText
  private lateinit var rgDifficulty: RadioGroup
  private lateinit var btnSave: Button
  private lateinit var btnCancel: Button
  private lateinit var btnEdit: Button
  private lateinit var btnDelete : Button
  private lateinit var btnBack: Button

  private var questionId:    String      = ""
  private var questionText:  String      = ""
  private var options:       List<String> = emptyList()
  private var correctIndexes: List<Int>  = emptyList()
  private var difficulty:    String      = ""


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_edit_delete_drill)

    //from viewdrillquestions
//    intent.putExtra("question_id", selectedQuestion.id)
//    intent.putExtra("theme", selectedQuestion.theme)
//    intent.putExtra("question", selectedQuestion.question)
//    intent.putStringArrayListExtra("options", ArrayList(selectedQuestion.options))
//    intent.putIntegerArrayListExtra("correct_indexes", ArrayList(selectedQuestion.correct_indexes))
//    intent.putExtra("difficulty", selectedQuestion.difficulty)
//    intent.putExtra("type", selectedQuestion.type)

    fStore = FirebaseFirestore.getInstance()

    initViews()
    getIntentData()
    populateFields()

    onView()

    btnEdit.setOnClickListener { onEdit() }
    btnCancel.setOnClickListener {
      populateFields()
      onView()
    }
    btnSave.setOnClickListener { saveDrill() }
    btnDelete.setOnClickListener { deleteDrill() }
    btnBack.setOnClickListener { finish() }

  }

  private fun initViews() {
    etDrillQuestion = findViewById(R.id.etDrillQuestion)
    cbOption1 = findViewById(R.id.cbOption1)
    etEditOption1 = findViewById(R.id.etEditOption1)
    cbOption2 = findViewById(R.id.cbOption2)
    etEditOption2 = findViewById(R.id.etEditOption2)
    cbOption3 = findViewById(R.id.cbOption3)
    etEditOption3 = findViewById(R.id.etEditOption3)
    cbOption4 = findViewById(R.id.cbOption4)
    etEditOption4 = findViewById(R.id.etEditOption4)
    cbOption5 = findViewById(R.id.cbOption5)
    etEditOption5 = findViewById(R.id.etEditOption5)
    rgDifficulty = findViewById(R.id.rgDifficulty)
    btnSave = findViewById(R.id.btnSave)
    btnCancel = findViewById(R.id.btnCancel)
    btnEdit = findViewById(R.id.btnEdit)
    btnDelete = findViewById(R.id.btnDelete)
    btnBack = findViewById(R.id.btnBack)
  }

  private fun getIntentData() {
    questionId     = intent.getStringExtra("question_id") ?: ""
    questionText   = intent.getStringExtra("question") ?: ""
    options        = intent.getStringArrayListExtra("options") ?: emptyList()
    correctIndexes = intent.getIntegerArrayListExtra("correct_indexes") ?: emptyList()
    difficulty     = intent.getStringExtra("difficulty") ?: ""
  }

  private fun populateFields() {
    //question text
    etDrillQuestion.setText(questionText)

    //options
    val editOptions = listOf(etEditOption1, etEditOption2, etEditOption3, etEditOption4, etEditOption5)
    //checkbox
    val boxes = listOf(cbOption1, cbOption2, cbOption3, cbOption4, cbOption5)

    options.forEachIndexed { i, text ->
      if (i < editOptions.size) {
        editOptions[i].setText(text)
        boxes[i].isChecked = correctIndexes.contains(i)
      }
    }

    val diff = difficulty.trim().lowercase()

    //difficulty
    val toCheck = when (diff) {
      "easy"   -> R.id.rbEasy
      "medium" -> R.id.rbMedium
      "hard"   -> R.id.rbHard
      else     -> null
    }

    if (toCheck != null) {
      rgDifficulty.check(toCheck)
    } else {
      rgDifficulty.clearCheck()
    }
  }

  private fun onEdit() {
    btnSave.visibility = View.VISIBLE
    btnCancel.visibility = View.VISIBLE

    btnEdit.visibility = View.GONE
    btnDelete.visibility = View.GONE

    btnBack.isEnabled = false

    toggleInputs(enabled = true)
  }

  private fun onView() {
    btnSave.visibility = View.GONE
    btnCancel.visibility = View.GONE

    btnEdit.visibility = View.VISIBLE
    btnDelete.visibility = View.VISIBLE

    btnBack.isEnabled = true

    toggleInputs(enabled = false)
  }

  private fun toggleInputs(enabled: Boolean) {
    //checkbox
    listOf(cbOption1, cbOption2, cbOption3, cbOption4, cbOption5)
      .forEach { it.isEnabled = enabled }

    // edit texts
    listOf(
      etDrillQuestion,
      etEditOption1, etEditOption2, etEditOption3, etEditOption4, etEditOption5
    ).forEach { et ->
      et.isEnabled = enabled
      et.isFocusable = enabled
      et.isFocusableInTouchMode = enabled
      et.isCursorVisible = enabled
    }

    rgDifficulty.isEnabled = enabled
    for (i in 0 until rgDifficulty.childCount) {
      val child = rgDifficulty.getChildAt(i)
      child.isEnabled = enabled
    }
  }

  private fun saveDrill() {
    val newQuestion = etDrillQuestion.text.toString().trim()
    val newOptions = listOf(
      etEditOption1.text.toString().trim(),
      etEditOption2.text.toString().trim(),
      etEditOption3.text.toString().trim(),
      etEditOption4.text.toString().trim(),
      etEditOption5.text.toString().trim()
    )
    val newCorrect = listOf(cbOption1, cbOption2, cbOption3, cbOption4, cbOption5)
      .mapIndexedNotNull { idx, cb ->
        if (cb.isChecked)
          idx
        else
          null
      }

    val newDiff = when (rgDifficulty.checkedRadioButtonId) {
      R.id.rbEasy   -> "easy"
      R.id.rbMedium -> "medium"
      R.id.rbHard   -> "hard"
      else          -> ""
    }

    if (newQuestion.isEmpty()) {
      Toast.makeText(this, "Question cannot be empty", Toast.LENGTH_SHORT).show()
      return
    }
    if (newOptions.any { it.isEmpty() }) {
      Toast.makeText(this, "All options must be filled", Toast.LENGTH_SHORT).show()
      return
    }
    if (newCorrect.isEmpty()) {
      Toast.makeText(this, "Select at least one correct answer", Toast.LENGTH_SHORT).show()
      return
    }
    if (newDiff.isEmpty()) {
      Toast.makeText(this, "Pick a difficulty", Toast.LENGTH_SHORT).show()
      return
    }

    val updates = mapOf(
      "question" to newQuestion,
      "options" to newOptions,
      "correct_indexes" to newCorrect,
      "difficulty" to newDiff,
      "updated_at" to FieldValue.serverTimestamp()
    )

    fStore.collection("questions")
      .document(questionId)
      .update(updates)
      .addOnSuccessListener {
        Toast.makeText(this, "Drill saved!", Toast.LENGTH_SHORT).show()

        questionText = newQuestion
        options = newOptions
        correctIndexes = newCorrect
        difficulty = newDiff

        populateFields()
        onView()
      }
      .addOnFailureListener { e ->
        Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
      }
  }

  private fun deleteDrill() {
    AlertDialog.Builder(this)
      .setTitle("Delete Drill")
      .setMessage("Are you sure you want to delete this drill? This cannot be undone.")
      .setPositiveButton("Delete") { _, _ ->
        fStore.collection("questions")
          .document(questionId)
          .delete()
          .addOnSuccessListener {
            Toast.makeText(this, "Drill deleted", Toast.LENGTH_SHORT).show()
            finish()
          }
          .addOnFailureListener { e ->
            Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
          }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }
}