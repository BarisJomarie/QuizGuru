package com.example.quizguru

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AddQuestionActivity : AppCompatActivity() {

  private lateinit var fAuth: FirebaseAuth
  private lateinit var fStore: FirebaseFirestore

  private lateinit var questionLayout: LinearLayout
  private lateinit var drillLayout: LinearLayout
  private lateinit var switchToDrill: Button
  private lateinit var switchToQuestion: Button

  private lateinit var videoSelector: Spinner
  private val videoList = mutableListOf<String>()
  private lateinit var back: Button

  //Drill
  private lateinit var drillQuestion: EditText
  private lateinit var drillOption1: EditText
  private lateinit var drillOption2: EditText
  private lateinit var drillOption3: EditText
  private lateinit var drillOption4: EditText
  private lateinit var drillOption5: EditText
  private lateinit var cbDrillOption1: CheckBox
  private lateinit var cbDrillOption2: CheckBox
  private lateinit var cbDrillOption3: CheckBox
  private lateinit var cbDrillOption4: CheckBox
  private lateinit var cbDrillOption5: CheckBox
  private lateinit var drillDifficulty: RadioGroup
  private lateinit var addDrill: Button
  private lateinit var clearDrill: Button
  //Question
  private lateinit var question: EditText
  private lateinit var q_option_a: EditText
  private lateinit var q_option_b: EditText
  private lateinit var q_option_c: EditText
  private lateinit var q_option_d: EditText
  private lateinit var correct_a: RadioGroup
  private lateinit var difficulty: RadioGroup
  private lateinit var fun_facts: EditText
  private lateinit var references: EditText
  private lateinit var addMultiQ: Button
  private lateinit var clear: Button

  private lateinit var loadingOverlay: LinearLayout
  private lateinit var error: TextView
  private lateinit var error1: TextView
  private lateinit var videoErr: TextView

  private var valid = true
  private val videoMap = mutableMapOf<String, VideoItem>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_add_question)

    //Initialize Firebase instances
    fAuth = FirebaseAuth.getInstance()
    fStore = FirebaseFirestore.getInstance()

    //UI
    videoSelector = findViewById(R.id.spVideoSelector)
    back = findViewById(R.id.btnBack)
    questionLayout = findViewById(R.id.questionLayout)
    drillLayout = findViewById(R.id.drillLayout)
    switchToDrill = findViewById(R.id.btnSwitchToDrill)
    switchToQuestion = findViewById(R.id.btnSwitchtoQuestion)

    //Drill
    drillQuestion = findViewById(R.id.etDrillQuestion)
    drillOption1 = findViewById(R.id.etDrillOption1)
    drillOption2 = findViewById(R.id.etDrillOption2)
    drillOption3 = findViewById(R.id.etDrillOption3)
    drillOption4 = findViewById(R.id.etDrillOption4)
    drillOption5 = findViewById(R.id.etDrillOption5)
    cbDrillOption1 = findViewById(R.id.cbDrill1)
    cbDrillOption2 = findViewById(R.id.cbDrill2)
    cbDrillOption3 = findViewById(R.id.cbDrill3)
    cbDrillOption4 = findViewById(R.id.cbDrill4)
    cbDrillOption5 = findViewById(R.id.cbDrill5)
    drillDifficulty = findViewById(R.id.rgDrillDifficulty)
    addDrill = findViewById(R.id.btnDrillAdd)
    clearDrill = findViewById(R.id.btnDrillClear)
    //Question
    question = findViewById(R.id.etQuestion)
    q_option_a = findViewById(R.id.etOptionA)
    q_option_b = findViewById(R.id.etOptionB)
    q_option_c = findViewById(R.id.etOptionC)
    q_option_d = findViewById(R.id.etOptionD)
    correct_a = findViewById(R.id.rdCorrectAnswer)
    difficulty = findViewById(R.id.rdDifficulty)
    fun_facts = findViewById(R.id.etFacts)
    references = findViewById(R.id.etReferences)
    addMultiQ = findViewById(R.id.btnAddQuestion)
    clear = findViewById(R.id.btnClear)


    loadingOverlay = findViewById(R.id.loadingOverlay)
    error = findViewById(R.id.tvRadioErr)
    error1 = findViewById(R.id.tvRadioErr1)
    videoErr = findViewById(R.id.tvVideoErr)

    val receivedVideo = intent.getStringExtra("video_uid") ?: ""

    val drillCheckbox = listOf(cbDrillOption1, cbDrillOption2, cbDrillOption3, cbDrillOption4, cbDrillOption5)
    for (cb in drillCheckbox) {
      cb.setOnCheckedChangeListener { _, _ ->
        updateCheckboxStates(drillCheckbox)
      }
    }


    if(receivedVideo == "") {
      loadVideos()
    } else {
      loadSingleVideo(receivedVideo)
    }


    difficulty.setOnCheckedChangeListener { _, _ ->
      error.text = ""
      error1.text = ""
    }

    switchToDrill.setOnClickListener {
      switchToDrill.visibility = View.GONE
      switchToQuestion.visibility = View.VISIBLE

      questionLayout.visibility = View.GONE
      drillLayout.visibility = View.VISIBLE
    }

    switchToQuestion.setOnClickListener {
      switchToDrill.visibility = View.VISIBLE
      switchToQuestion.visibility = View.GONE

      questionLayout.visibility = View.VISIBLE
      drillLayout.visibility = View.GONE
    }

    back.setOnClickListener {
      finish()
    }

    addMultiQ.setOnClickListener {
      addQuestion1()
    }

    clear.setOnClickListener {
      resetFields()
    }

    addDrill.setOnClickListener {
      addQuestion2()
    }

    clearDrill.setOnClickListener {
      resetDrill()
    }
  }

  private fun checkField(textField: EditText) {
    if(textField.text.toString().trim().isEmpty()) {
      textField.error = "This field is required"
      valid = false
    }
  }

  private fun resetUI() {
    loadingOverlay.visibility = View.GONE
    addMultiQ.isEnabled = true
    clear.isEnabled = true
  }

  private fun resetFields() {
    question.text.clear()
    q_option_a.text.clear()
    q_option_b.text.clear()
    q_option_c.text.clear()
    q_option_d.text.clear()
    correct_a.clearCheck()
    difficulty.clearCheck()
    fun_facts.text.clear()
    references.text.clear()
    videoSelector.setSelection(0)
    resetUI()
  }

  private fun resetDrill() {
    drillQuestion.text.clear()
    drillOption1.text.clear()
    drillOption2.text.clear()
    drillOption3.text.clear()
    drillOption4.text.clear()
    drillOption5.text.clear()

    // Uncheck and re-enable all checkboxes
    val drillCheckboxes = listOf(cbDrillOption1, cbDrillOption2, cbDrillOption3, cbDrillOption4, cbDrillOption5)
    for (cb in drillCheckboxes) {
      cb.isChecked = false
      cb.isEnabled = true
    }

    // Clear difficulty selection
    drillDifficulty.clearCheck()

    // Hide any error messages and restore buttons
    error.text = ""
    videoErr.text = ""
    loadingOverlay.visibility = View.GONE
    addDrill.isEnabled = true
    clearDrill.isEnabled = true
  }

  // Multiple Choice
  private fun addQuestion1() {
    valid = true

    //validate text fields
    checkField(question)
    checkField(q_option_a)
    checkField(q_option_b)
    checkField(q_option_c)
    checkField(q_option_d)
    checkField(fun_facts)
    checkField(references)

    //difficulty
    val selectedId = difficulty.checkedRadioButtonId
    //correct answer
    val selectedId1 = correct_a.checkedRadioButtonId

    //validate difficulty and correct_ans selection
    when {
      selectedId == -1 -> {
        valid = false
        error.text = "Please Select a difficulty level"
        return
      }
      selectedId1 == -1 -> {
        valid = false
        error1.text = "Please Select a the correct answer"
        return
      }
    }

    //select radio
    val selectedRadio = findViewById<RadioButton>(selectedId)
    val selectedDifficulty = selectedRadio.text.toString().lowercase()

    val selectedRadio1 = findViewById<RadioButton>(selectedId1)
    val selectedCorrectA = selectedRadio1.text.toString().lowercase()

    val selectedVideoItem = selectedVideo()

    if (selectedVideoItem == null) {
      videoErr.text = "Select a video"
      valid = false
    }

    if(valid) {
      loadingOverlay.visibility = View.VISIBLE
      addMultiQ.isEnabled = false
      clear.isEnabled = false

      val options = mapOf(
        "a" to q_option_a.text.toString().trim(),
        "b" to q_option_b.text.toString().trim(),
        "c" to q_option_c.text.toString().trim(),
        "d" to q_option_d.text.toString().trim()
      )

      val questionData = hashMapOf(
        "video_uid" to selectedVideoItem!!.uid,
        "theme" to selectedVideoItem.theme,
        "question" to question.text.toString().trim(),
        "options" to options,
        "answer" to selectedCorrectA,
        "difficulty" to selectedDifficulty,
        "facts" to fun_facts.text.toString().trim(),
        "references" to references.text.toString().trim(),
        "type" to "test",
        "question_type" to "multiple_choice",
        "uploaded_by" to fAuth.currentUser?.uid,
        "created_at" to FieldValue.serverTimestamp(),
        "updated_at" to FieldValue.serverTimestamp()
      )

      fStore.collection("questions").add(questionData)
        .addOnSuccessListener {
          AlertDialog.Builder(this)
            .setTitle("Added a Question")
            .setMessage("Added successfully.")
            .setPositiveButton("Nice!", null)
            .show()

          resetFields()
        }
        .addOnFailureListener {
          AlertDialog.Builder(this)
            .setTitle("Failed adding question")
            .setMessage("Added unsuccessfully")
            .setNegativeButton("OK", null)
            .show()
          resetUI()
        }
    } else {
      resetUI()
      return
    }
  }

  //Fill In the blanks
  private fun addQuestion2() {
    valid = true

    //edit fields
    checkField(drillQuestion)
    checkField(drillOption1)
    checkField(drillOption2)
    checkField(drillOption3)
    checkField(drillOption4)
    checkField(drillOption5)

    //difficulty
    val selectedDiffId = drillDifficulty.checkedRadioButtonId
    if (selectedDiffId == -1) {
      Toast.makeText(this, "Select a difficulty", Toast.LENGTH_SHORT).show()
      valid = false
    }

    //checkbox
    val drillCheckbox = listOf(cbDrillOption1, cbDrillOption2, cbDrillOption3, cbDrillOption4, cbDrillOption5)
    val checkedOptions = drillCheckbox.filter { it.isChecked }
    if (checkedOptions.isEmpty()) {
      Toast.makeText(this, "Select at least one correct answer", Toast.LENGTH_SHORT).show()
      valid = false
    }

    val selectedVideo = selectedVideo()
    if (selectedVideo == null) {
      Toast.makeText(this, "Select up to 3 correct answers.", Toast.LENGTH_SHORT).show()
      valid = false
    }

    if (valid) {
      val options = listOf(
        drillOption1.text.toString().trim(),
        drillOption2.text.toString().trim(),
        drillOption3.text.toString().trim(),
        drillOption4.text.toString().trim(),
        drillOption5.text.toString().trim()
      )

      val correctIndexes = drillCheckbox.mapIndexedNotNull { index, cb ->
        if (cb.isChecked)
          index
        else
          null
      }

      val selectedDiff = findViewById<RadioButton>(selectedDiffId).text.toString().lowercase()

      val drillData = hashMapOf(
        "video_uid" to selectedVideo!!.uid,
        "theme" to selectedVideo.theme,
        "question" to drillQuestion.text.toString().trim(),
        "options" to options,
        "correct_indexes" to correctIndexes,
        "difficulty" to selectedDiff,
        "type" to "drill",
        "question_type" to "fill_in_the_blanks",
        "uploaded_by" to fAuth.currentUser?.uid,
        "created_at" to FieldValue.serverTimestamp(),
        "updated_at" to FieldValue.serverTimestamp()
      )

      loadingOverlay.visibility = View.VISIBLE
      addDrill.isEnabled = false
      clearDrill.isEnabled = false

      fStore.collection("questions")
        .add(drillData)
        .addOnSuccessListener {
          AlertDialog.Builder(this)
            .setTitle("Drill Question Added")
            .setMessage("Added successfully.")
            .setPositiveButton("OK", null)
            .show()

          resetDrill()
        }
        .addOnFailureListener {
          AlertDialog.Builder(this)
            .setTitle("Failed")
            .setMessage("Drill question not added.")
            .setNegativeButton("OK", null)
            .show()
          loadingOverlay.visibility = View.GONE
          addDrill.isEnabled = true
          clearDrill.isEnabled = true
        }
    }
  }

  //checkbox limit to 3
  private fun updateCheckboxStates(checkBoxes: List<CheckBox>) {
    val checkedCount = checkBoxes.count { it.isChecked }

    for (cb in checkBoxes) {
      cb.isEnabled = checkedCount < 3 || cb.isChecked
    }
  }

  private fun loadVideos() {
    val default = "-- Select Video --"
    videoList.add(default)

    fStore.collection("videos")
      .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
      .get()
      .addOnSuccessListener { result ->
        for(doc in result) {
          val videoTitle = doc.getString("title")
          val videoUid = doc.id
          if(videoTitle != null && doc.contains("theme")) {
            val videoTheme = doc.getString("theme") ?: ""
            videoMap[videoTitle] = VideoItem(videoUid, videoTheme)
            videoList.add(videoTitle)
          }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, videoList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        videoSelector.adapter = adapter
      }
      .addOnFailureListener { e ->
        Toast.makeText(this, "Failed to load themes", Toast.LENGTH_SHORT).show()
      }
  }

  private fun loadSingleVideo(videoUid: String) {
    fStore.collection("videos").document(videoUid).get()
      .addOnSuccessListener { doc ->
        if (doc.exists()) {
          val videoTitle = doc.getString("title") ?: "Unknown Title"
          val videoTheme = doc.getString("theme") ?: ""

          videoMap[videoTitle] = VideoItem(videoUid, videoTheme)
          videoList.clear()
          videoList.add(videoTitle)

          val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, videoList)
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
          videoSelector.adapter = adapter
          videoSelector.setSelection(0)
          videoSelector.isEnabled = false
        } else {
          Toast.makeText(this, "Video not found", Toast.LENGTH_SHORT).show()
          finish()
        }
      }
      .addOnFailureListener {
        Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show()
      }
  }


  //access selected theme
  private fun selectedVideo(): VideoItem? {
    val selectedTitle = videoSelector.selectedItem.toString()
    return if (selectedTitle != "-- Select Video --") {
      videoMap[selectedTitle]
    } else {
      null
    }
  }

}