package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainGameActivity : AppCompatActivity() {

  private lateinit var questionCount: TextView
  private lateinit var questionText: TextView
  private lateinit var questions: MutableList<Question>
  private lateinit var btnA: Button
  private lateinit var btnB: Button
  private lateinit var btnC: Button
  private lateinit var btnD: Button
  private lateinit var btnNext: Button
  private var orderedVideo: List<String> = emptyList()
  private lateinit var loadingOverlay: LinearLayout

  private lateinit var videoTitleMap: Map<String, String>
  private lateinit var theme: String
  private lateinit var difficulty: String

  private var currentIndex = 0

  private lateinit var fStore: FirebaseFirestore
  private lateinit var fAuth: FirebaseAuth

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_game)

    fStore = FirebaseFirestore.getInstance()
    fAuth = FirebaseAuth.getInstance()

    questionCount = findViewById(R.id.tvQuestionCount)
    questionText = findViewById(R.id.tvQuestion)
    btnA = findViewById(R.id.btnA)
    btnB = findViewById(R.id.btnB)
    btnC = findViewById(R.id.btnC)
    btnD = findViewById(R.id.btnD)
    btnNext = findViewById(R.id.btnNext)
    loadingOverlay = findViewById(R.id.loadingOverlay)

    theme = intent.getStringExtra("selectedTheme") ?: ""
    difficulty = intent.getStringExtra("selectedDifficulty") ?: ""



    fetchPretestQuestions(theme, difficulty)

    btnA.setOnClickListener { onOptionSelected("a") }
    btnB.setOnClickListener { onOptionSelected("b") }
    btnC.setOnClickListener { onOptionSelected("c") }
    btnD.setOnClickListener { onOptionSelected("d") }
    btnNext.isEnabled = false
    resetOptionButtons()

    btnNext.setOnClickListener {
      if (::questions.isInitialized && questions.isNotEmpty()) {
        val currentQuestion = questions[currentIndex]
        val correctAnswer = currentQuestion.answer.lowercase()
        val selected = currentQuestion.selectedAnswer?.lowercase()

        //highlight correct/wrong
        resetOptionButtons()
        disableOptionButtons()

        val selectedBtn = when (selected) {
          "a" -> btnA
          "b" -> btnB
          "c" -> btnC
          "d" -> btnD
          else -> null
        }

        val correctBtn = when (correctAnswer) {
          "a" -> btnA
          "b" -> btnB
          "c" -> btnC
          "d" -> btnD
          else -> null
        }

        if (selected == correctAnswer) {
          selectedBtn?.setBackgroundResource(R.drawable.selected_correct_option)
          selectedBtn?.setTextColor(ContextCompat.getColor(this, R.color.black))
        } else {
          correctBtn?.setBackgroundResource(R.drawable.selected_correct_option)
          correctBtn?.setTextColor(ContextCompat.getColor(this, R.color.black))
          selectedBtn?.setBackgroundResource(R.drawable.selected_wrong_option)
          selectedBtn?.setTextColor(ContextCompat.getColor(this, R.color.black))
        }

        val grayColor = ContextCompat.getColor(this, R.color.gray)
        listOf("a", "b", "c", "d").forEach { opt ->
          if (opt != selected && opt != correctAnswer) {
            getButtonByOption(opt)?.setTextColor(grayColor)
          }
        }

        btnNext.isEnabled = false

        //delay
        btnNext.postDelayed({
          currentIndex++

          //fade animation lang hahahha
          questionText.alpha = 0f
          questionText.animate().alpha(1f).setDuration(300).start()

          if (currentIndex < questions.size) {
            showQuestion()
            resetOptionButtons()
            enableOptionButtons()
            if (currentIndex + 1 == questions.size)
              btnNext.text = "Finish Pre-Test"
          } else {
            val score = calculateScore(questions)
            PrePostManager.savePreTestQuestions(questions)
            PrePostManager.savePreTestScore(score, this)
            // Add pretest result in firestore
            addPretestAnalysis()
            val videoIds = ArrayList(orderedVideo.take(3))
            val intent = Intent(this, MainGameVideoActivity::class.java)
            intent.putStringArrayListExtra("video_ids", ArrayList(videoIds))
            intent.putExtra("current_index", 0)
            intent.putExtra("difficulty", difficulty)
            intent.putExtra("theme", theme)
            startActivity(intent)
          }
        }, 1500)
      }
    }
  }

  private fun fetchPretestQuestions(theme: String, difficulty: String) {
    Log.d("DEBUG", "Selected Theme: $theme, Difficulty: $difficulty")
    showLoading()
    fStore.collection("videos")
      .whereEqualTo("theme", theme)
      .orderBy("order")
      .get()
      .addOnSuccessListener { video ->
        if (video.isEmpty) {
          Log.d("DEBUG", "No test videos found for theme: $theme")
          hideLoading()
          return@addOnSuccessListener
        }

        videoTitleMap = video.documents.associate {
          it.id to (it.getString("title") ?: "Untitled")
        }

        orderedVideo = video.documents.mapNotNull { it.id }
        Log.d("DEBUG", "Fetched videos: $orderedVideo")

        fStore.collection("questions")
          .whereEqualTo("difficulty", difficulty)
          .whereEqualTo("type", "test")
          .get()
          .addOnSuccessListener { result ->
            val allQuestions = result.documents.mapNotNull { it.toObject(Question::class.java) }
            Log.d("DEBUG", "Fetched ${allQuestions.size} total questions")

            questions = pick15Questions(allQuestions, orderedVideo)
            Log.d("DEBUG", "Selected ${questions.size} pretest questions")

            if (questions.isEmpty()) {
              Log.d("DEBUG", "No questions matched the video_uid filter.")
            }

            val breakdown = buildPreTestBreakdown(questions, orderedVideo, videoTitleMap)

            showQuestion()
            hideLoading()
          }
      }
  }

  private fun pick15Questions(
    allQuestions: List<Question>,
    orderedVideo: List<String>
  ): MutableList<Question> {
    val grouped = allQuestions.groupBy { it.video_uid }

    // Pick questions from the first 3 video IDs only
    val selected = orderedVideo.take(3).flatMap { videoId ->
      grouped[videoId]?.shuffled()?.take(5) ?: emptyList()
    }
    Log.d("DEBUG", "Selected ${selected.size} questions from videos: $orderedVideo")

    return selected.toMutableList()
  }

  private fun showQuestion() {
    btnNext.isEnabled = false
    if (currentIndex >= questions.size) {
      //Done with pretest
      //save for posttest
      //basically failsafe to, kapag di gumana main controller eto yung sasalo
      PrePostManager.savePreTestQuestions(questions)
      addPretestAnalysis()
      val videoIds = ArrayList(orderedVideo.take(3))
      val intent = Intent(this, MainGameVideoActivity::class.java)
      intent.putStringArrayListExtra("video_ids", ArrayList(videoIds))
      intent.putExtra("current_index", 0)
      intent.putExtra("difficulty", difficulty)
      intent.putExtra("theme", theme)
      startActivity(intent)
      return
    }

    val q = questions[currentIndex]
    questionCount.text = "Question ${currentIndex + 1} of ${questions.size}"
    questionText.text = q.question
    btnA.text = q.options["a"]
    btnB.text = q.options["b"]
    btnC.text = q.options["c"]
    btnD.text = q.options["d"]
  }

  private fun addPretestAnalysis() {
    val userId = fAuth.currentUser?.uid ?: "anonymous"
    val timestamp = FieldValue.serverTimestamp()

    val videoBreakdown = buildPreTestBreakdown(questions, orderedVideo, videoTitleMap)

    val preTestData = hashMapOf(
      "user_id" to userId,
      "theme" to theme,
      "difficulty" to difficulty,
      "timestamp" to timestamp,
      "attempt_type" to "pre_test",
      "video_breakdown" to videoBreakdown,
      "score" to questions.count { it.selectedAnswer?.lowercase() == it.answer.lowercase() },
      "total_questions" to questions.size,
      "percentage" to ((questions.count { it.selectedAnswer?.lowercase() == it.answer.lowercase() }.toDouble() / questions.size) * 100)
    )

    fStore.collection("attempt_history")
      .add(preTestData)
      .addOnSuccessListener { Log.d("PreTest", "Pre-test saved") }
      .addOnFailureListener { e -> Log.e("PreTest", "Error saving pre-test", e) }
  }

  private fun buildPreTestBreakdown(
    questions: List<Question>,
    orderedVideo: List<String>,
    videoTitles: Map<String, String>
  ): Map<String, Any> {
    val grouped = questions.groupBy { it.video_uid }

    return orderedVideo.take(3).mapIndexed { index, vid ->
      val videoKey = "video${index + 1}"
      val videoQuestions = grouped[vid]?.map {
        mapOf(
          "question" to it.question,
          "selected_answer" to it.selectedAnswer,
          "correct_answer" to it.answer,
          "is_correct" to (it.selectedAnswer?.lowercase() == it.answer.lowercase())
        )
      } ?: emptyList()

      videoKey to mapOf(
        "video_uid" to vid,
        "video_title" to (videoTitles[vid] ?: "Unknown"),
        "questions" to videoQuestions
      )
    }.toMap()
  }

  private fun onOptionSelected(selected: String) {
    questions[currentIndex].selectedAnswer = selected
    resetOptionButtons()

    //visually show selection
    when (selected) {
      "a" -> btnA.setBackgroundResource(R.drawable.selected_option)
      "b" -> btnB.setBackgroundResource(R.drawable.selected_option)
      "c" -> btnC.setBackgroundResource(R.drawable.selected_option)
      "d" -> btnD.setBackgroundResource(R.drawable.selected_option)
    }

    btnNext.isEnabled = true
  }

  private fun disableOptionButtons() {
    btnA.isEnabled = false
    btnB.isEnabled = false
    btnC.isEnabled = false
    btnD.isEnabled = false
  }

  private fun enableOptionButtons() {
    btnA.isEnabled = true
    btnB.isEnabled = true
    btnC.isEnabled = true
    btnD.isEnabled = true
  }



  private fun resetOptionButtons() {
    btnA.setBackgroundResource(R.drawable.default_option)
    btnB.setBackgroundResource(R.drawable.default_option)
    btnC.setBackgroundResource(R.drawable.default_option)
    btnD.setBackgroundResource(R.drawable.default_option)

    val defaultText = ContextCompat.getColor(this, R.color.button_default_text)
    btnA.setTextColor(defaultText)
    btnB.setTextColor(defaultText)
    btnC.setTextColor(defaultText)
    btnD.setTextColor(defaultText)
  }

  private fun getButtonByOption(option: String): Button? {
    return when (option) {
      "a" -> btnA
      "b" -> btnB
      "c" -> btnC
      "d" -> btnD
      else -> null
    }
  }

  private fun showLoading() {
    loadingOverlay.visibility = View.VISIBLE
  }

  private fun hideLoading() {
    loadingOverlay.visibility = View.GONE
  }

  private fun calculateScore(questions: List<Question>): Int {
    return questions.count { it.selectedAnswer?.lowercase() == it.answer.lowercase() }
  }



}