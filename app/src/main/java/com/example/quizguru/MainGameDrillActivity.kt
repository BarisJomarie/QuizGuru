package com.example.quizguru

import DrillDragListener
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quizguru.model.DrillQuestion
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.firestore.FirebaseFirestore

class MainGameDrillActivity : AppCompatActivity() {

  private lateinit var fStore: FirebaseFirestore

  private lateinit var drillQuestion: TextView
  private lateinit var drillDropContainer: FlexboxLayout
  private lateinit var drillOptions: LinearLayout

  private lateinit var congratsOverlay: LinearLayout
  private lateinit var next: Button
  private lateinit var retryOverlay: LinearLayout
  private lateinit var retry: Button
  private lateinit var submit: Button

  private lateinit var drillList: List<DrillQuestion>
  private var currentDrillIndex = 0
  private lateinit var currentDrill: DrillQuestion
  private lateinit var answerSlots: MutableList<TextView>

  private lateinit var videoIds: ArrayList<String>
  private val drillAnswerMap = mutableMapOf<String, MutableList<DrillAnswer>>()

  private var currentVideoIndex = 0


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_game_drill)

    fStore = FirebaseFirestore.getInstance()

    drillQuestion = findViewById(R.id.drillQuestion)
    drillDropContainer = findViewById(R.id.drillDropContainer)
    drillOptions = findViewById(R.id.dragOptions)
    congratsOverlay = findViewById(R.id.congratsOverlay)
    next = findViewById(R.id.btnNext)
    retryOverlay = findViewById(R.id.retryOverlay)
    retry = findViewById(R.id.btnRetry)
    submit = findViewById(R.id.btnSubmit)


    videoIds = intent.getStringArrayListExtra("video_ids") ?: arrayListOf()
    currentVideoIndex = intent.getIntExtra("current_index", 0)
    val difficulty = intent.getStringExtra("difficulty") ?: ""
    val currentVideo = videoIds[currentVideoIndex]
    fetchDrill(currentVideo, difficulty)
    Log.d("DEBUG", "Received difficulty=$difficulty")


    next.setOnClickListener {
      congratsOverlay.visibility = View.GONE
      currentDrillIndex++
      if (currentDrillIndex < drillList.size) {
        displayDrillQuestion(drillList[currentDrillIndex])
      } else {
        nextProgress(videoIds, currentVideoIndex)
      }
    }


    retry.setOnClickListener {
      retryOverlay.visibility = View.GONE
      displayDrillQuestion(currentDrill)
    }

    submit.setOnClickListener {
      val filledAnswer = answerSlots.map { it.text.toString().trim() }
      val correctAnswer = currentDrill.correct_indexes.map { currentDrill.options[it].trim() }
      val isCorrect = filledAnswer.sorted() == correctAnswer.sorted()

      //Track attempt
      val videoId = videoIds[currentVideoIndex]
      val drillKey = currentDrill.question

      val drillList = drillAnswerMap.getOrPut(videoId) { mutableListOf() }
      val existing = drillList.find { it.question == drillKey }

      if (existing == null) {
        drillList.add(
          DrillAnswer(
            question = drillKey,
            attemptCount = 1,
            finalAnswer = filledAnswer,
            isCorrect = isCorrect
          )
        )
      } else {
        existing.attemptCount++
        existing.finalAnswer = filledAnswer
        existing.isCorrect = isCorrect
      }

      if (isCorrect) {
        congratsOverlay.visibility = View.VISIBLE
      } else {
        retryOverlay.visibility = View.VISIBLE
      }
    }

  }

  private fun fetchDrill(
    videoId: String,
    difficulty: String
  ) {
    Log.d("DEBUG", "Fetching drills for video_uid=$videoId, difficulty=$difficulty")
    fStore.collection("questions")
      .whereEqualTo("video_uid", videoId)
      .whereEqualTo("type", "drill")
      .whereEqualTo("difficulty", difficulty)
      .get()
      .addOnSuccessListener { result ->
        Log.d("DEBUG", "Fetched ${result.size()} drill documents")
        for (doc in result) {
          Log.d("DEBUG", "Doc ID: ${doc.id}, data: ${doc.data}")
        }
        val drillQuestions = result.documents.mapNotNull { it.toObject(DrillQuestion::class.java) }
        if (drillQuestions.isNotEmpty()) {
          drillList = drillQuestions.shuffled().take(3)
          currentDrillIndex = 0
          displayDrillQuestion(drillList[currentDrillIndex])
        } else {
          Toast.makeText(this, "No drill questions found.", Toast.LENGTH_SHORT).show()
          nextProgress(videoIds, intent.getIntExtra("current_index", 0) + 1)
        }
      }
      .addOnFailureListener {
        Toast.makeText(this, "Failed to load drills.", Toast.LENGTH_SHORT).show()
        nextProgress(videoIds, intent.getIntExtra("current_index", 0) + 1)
      }

  }

  private fun displayDrillQuestion(drill: DrillQuestion) {
    drillDropContainer.removeAllViews()
    drillOptions.removeAllViews()

    currentDrill = drill
    answerSlots = mutableListOf()

    val parts = drill.question.split("_")

    //build question with blanks
    for (i in parts.indices) {
      val part = TextView(this).apply {
        text = parts[i]
        textSize = 18f
        setPadding(4, 8, 4, 8)
        layoutParams = FlexboxLayout.LayoutParams(
          FlexboxLayout.LayoutParams.WRAP_CONTENT,
          FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
      }
      drillDropContainer.addView(part)

      if (i < parts.size - 1) {
        val dropSlot = TextView(this).apply {
          text = "___"
          textSize = 18f
          setPadding(16, 8, 16, 8)
          background = getDrawable(R.drawable.drop_slot_background)
          tag = "slot_$i"
          setOnDragListener(DrillDragListener())
          layoutParams = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
          )
        }
        drillDropContainer.addView(dropSlot)
        answerSlots.add(dropSlot)

      }
    }

    //draggable options
    drill.options.shuffled().forEach { option ->
      val dragOption = TextView(this).apply {
        text = option
        textSize = 18f
        setPadding(16, 8, 16, 8)
        background = getDrawable(R.drawable.draggable_background)
        setOnLongClickListener {
          val clipData = ClipData.newPlainText("text", text)
          val shadow = View.DragShadowBuilder(it)
          it.startDragAndDrop(clipData, shadow, it, 0)
          true
        }
      }
      drillOptions.addView(dragOption)
    }

    if (currentDrillIndex == drillList.size - 1) {
      if (currentVideoIndex == videoIds.size - 1) {
        // Last drill of the last video
        next.text = "Proceed to Post-Test"
      } else {
        // Last drill of a video
        next.text = "Proceed to Video ${currentVideoIndex+ 1}"
      }
    } else {
      next.text = "Next"
    }

  }

  private fun nextProgress(
    videoIds: ArrayList<String>,
    currentIndex: Int
  ) {
    val difficulty = intent.getStringExtra("difficulty") ?: ""
    val theme = intent.getStringExtra("theme") ?: ""
    val nextIndex = currentIndex + 1

    val combinedMap = PrePostManager.getDrillAnswers().toMutableMap()

    for ((videoId, newDrills) in drillAnswerMap) {
      val existing = combinedMap.getOrPut(videoId) { mutableListOf() }.toMutableList()

      for (newDrill in newDrills) {
        val found = existing.find { it.question == newDrill.question }
        if (found == null) {
          existing.add(newDrill)
        } else {
          found.attemptCount += newDrill.attemptCount
          found.finalAnswer = newDrill.finalAnswer
          found.isCorrect = newDrill.isCorrect
        }
      }

      combinedMap[videoId] = existing // Update back after mutation
    }

    PrePostManager.setDrillAnswers(combinedMap)

    if (nextIndex < videoIds.size) {
      val difficulty = intent.getStringExtra("difficulty") ?: ""
      val intent = Intent(this, MainGameVideoActivity::class.java)
      intent.putStringArrayListExtra("video_ids", videoIds)
      intent.putExtra("current_index", nextIndex)
      intent.putExtra("difficulty", difficulty)
      intent.putExtra("theme", theme)
      startActivity(intent)
      finish()
    } else {
      val intent = Intent(this, MainGamePostActivity::class.java)
      intent.putStringArrayListExtra("video_ids", videoIds)
      intent.putExtra("difficulty", difficulty)
      intent.putExtra("theme", theme)
      startActivity(intent)
      finish()
    }
  }

//  private fun setDrillAnswers(newAnswers: Map<String, MutableList<DrillAnswer>>) {
//    for ((videoId, newDrills) in newAnswers) {
//      val existingDrills = drillAnswerMap.getOrPut(videoId) { mutableListOf() }
//      for (newDrill in newDrills) {
//        val existing = existingDrills.find { it.question == newDrill.question }
//        if (existing == null) {
//          existingDrills.add(newDrill)
//        } else {
//          existing.attemptCount += newDrill.attemptCount
//          existing.finalAnswer = newDrill.finalAnswer
//          existing.isCorrect = newDrill.isCorrect
//        }
//      }
//    }
//  }
}