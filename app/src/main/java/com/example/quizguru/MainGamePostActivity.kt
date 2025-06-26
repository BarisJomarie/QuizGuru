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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainGamePostActivity : AppCompatActivity() {

  private lateinit var fStore: FirebaseFirestore
  private lateinit var fAuth: FirebaseAuth

  private lateinit var tvQuestionCount: TextView
  private lateinit var tvQuestion: TextView
  private lateinit var btnA: Button
  private lateinit var btnB: Button
  private lateinit var btnC: Button
  private lateinit var btnD: Button
  private lateinit var btnNext: Button

  private lateinit var loadingOverlay: LinearLayout

  private lateinit var questions: MutableList<Question>
  private lateinit var videoIds: ArrayList<String>
  private lateinit var difficulty: String

  private lateinit var theme: String
  private lateinit var orderedVideo: List<String>
  private lateinit var videoTitleMap: Map<String, String>
  private lateinit var drillAnswers: Map<String, List<DrillAnswer>>


  private var currentIndex = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_game_post)

    fStore = FirebaseFirestore.getInstance()
    fAuth = FirebaseAuth.getInstance()

    tvQuestionCount = findViewById(R.id.tvQuestionCount)
    tvQuestion = findViewById(R.id.tvQuestion)
    btnA = findViewById(R.id.btnA)
    btnB = findViewById(R.id.btnB)
    btnC = findViewById(R.id.btnC)
    btnD = findViewById(R.id.btnD)
    btnNext = findViewById(R.id.btnNext)
    loadingOverlay = findViewById(R.id.loadingOverlay)

    questions = PrePostManager.getPreTestQuestions().shuffled().toMutableList()
    videoIds = intent.getStringArrayListExtra("video_ids") ?: arrayListOf()
    difficulty = intent.getStringExtra("difficulty") ?: ""
    drillAnswers = PrePostManager.getDrillAnswers()
    theme = intent.getStringExtra("theme") ?: ""
    orderedVideo = videoIds.take(3)
    showLoading()
    fetchVideoTitles(orderedVideo) { titles ->
      hideLoading()
      videoTitleMap = titles
      showQuestion()
    }

    btnA.setOnClickListener { onOptionSelected("a") }
    btnB.setOnClickListener { onOptionSelected("b") }
    btnC.setOnClickListener { onOptionSelected("c") }
    btnD.setOnClickListener { onOptionSelected("d") }

    btnNext.setOnClickListener {
      if (::questions.isInitialized && questions.isNotEmpty()) {
        val currentQuestion = questions[currentIndex]
        val correctAnswer = currentQuestion.answer.lowercase()
        val selected = currentQuestion.selectedAnswer?.lowercase()

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

        disableOptionButtons()
        btnNext.isEnabled = false
        btnNext.postDelayed({
          currentIndex++

          tvQuestion.alpha = 0f
          tvQuestion.animate().alpha(1f).setDuration(300).start()

          if (currentIndex < questions.size) {
            showQuestion()
            resetOptionButtons()
            enableOptionButtons()
            if (currentIndex + 1 == questions.size)
              btnNext.text = "Complete the Quiz"
          } else {
            PrePostManager.savePostTestQuestions(questions)
            val previousScore = PrePostManager.getPreTestScore(this)
            addPostTestAnalysis(questions, drillAnswers, previousScore)

            val videoIds = intent.getStringArrayListExtra("video_ids") ?: arrayListOf()
            val fetchTasks = videoIds.map { videoId ->
              fStore.collection("videos")
                .document(videoId)
                .get()
            }

            Tasks.whenAllSuccess<DocumentSnapshot>(fetchTasks).addOnSuccessListener { documents ->
              val videoTitles = hashMapOf<String, String>()
              for (doc in documents) {
                val videoId = doc.id
                val title = doc.getString("title") ?: "Untitled"
                videoTitles[videoId] = title
              }

              val intent = Intent(this, MainGameResultActivity::class.java)
              intent.putParcelableArrayListExtra("post_test_results", ArrayList(questions))
              intent.putExtra("difficulty", difficulty)
              intent.putExtra("theme", theme)
              intent.putStringArrayListExtra("video_ids", videoIds)
              intent.putExtra("video_titles", HashMap(videoTitleMap))
              startActivity(intent)
              finish()
            }
          }
          enableOptionButtons()
        }, 1500)

      }
    }
  }

  private fun fetchVideoTitles(
    videoIds: List<String>,
    onResult: (Map<String, String>
      ) -> Unit) {
    fStore.collection("videos")
      .whereIn(FieldPath.documentId(), videoIds)
      .get()
      .addOnSuccessListener { snapshot ->
        val titles = snapshot.documents.associate {
          it.id to (it.getString("title") ?: "Untitled")
        }
        onResult(titles)
      }
      .addOnFailureListener {
        onResult(videoIds.associateWith { "Untitled" }) // fallback
      }
  }


  private fun showQuestion() {
    val q = questions[currentIndex]
    tvQuestionCount.text = "Question ${currentIndex + 1} of ${questions.size}"
    tvQuestion.text = q.question
    btnA.text = q.options["a"]
    btnB.text = q.options["b"]
    btnC.text = q.options["c"]
    btnD.text = q.options["d"]
  }

  private fun addPostTestAnalysis(
    questions: List<Question>,
    drillAnswers: Map<String, List<DrillAnswer>>,
    previousScore: Int
  ) {
    val userId = fAuth.currentUser?.uid ?: "anonymous"
    val timestamp = FieldValue.serverTimestamp()
    val score = questions.count { it.selectedAnswer?.lowercase() == it.answer.lowercase() }
    val total = questions.size
    val percentage = ((score.toDouble() / total) * 100)
    PrePostManager.savePostTestScore(score, this)

    val videoBreakdown = buildPostTestBreakdown(questions, orderedVideo, videoTitleMap, drillAnswers)

    val postTestData = hashMapOf(
      "user_id" to userId,
      "theme" to theme,
      "difficulty" to difficulty,
      "timestamp" to timestamp,
      "attempt_type" to "post_test",
      "video_breakdown" to videoBreakdown,
      "score" to score,
      "total_questions" to total,
      "percentage" to percentage,
      "result" to hashMapOf(
        "status" to if (score > previousScore) "improved"
        else if (score < previousScore) "declined"
        else "same",
        "previous_score" to previousScore,
        "score_difference" to (score - previousScore)
      )
    )

    fStore.collection("attempt_history")
      .add(postTestData)
      .addOnSuccessListener { Log.d("PostTest", "Post-test saved") }
      .addOnFailureListener { e -> Log.e("PostTest", "Error saving post-test", e) }
  }

  private fun buildPostTestBreakdown(
    questions: List<Question>,
    orderedVideo: List<String>,
    videoTitles: Map<String, String>,
    drillAnswers: Map<String, List<DrillAnswer>>
  ): Map<String, Any> {
    val groupedQuestions = questions.groupBy { it.video_uid }

    return orderedVideo.take(3).mapIndexed { index, vid ->
      val videoKey = "video${index + 1}"
      val videoQuestions = groupedQuestions[vid]?.map {
        mapOf(
          "question" to it.question,
          "selected_answer" to it.selectedAnswer,
          "correct_answer" to it.answer,
          "is_correct" to (it.selectedAnswer?.lowercase() == it.answer.lowercase())
        )
      } ?: emptyList()

      val drills = drillAnswers[vid]?.map { drill ->
        mapOf(
          "question" to drill.question,
          "attempt_count" to drill.attemptCount,
          "final_answer" to drill.finalAnswer,
          "is_correct" to drill.isCorrect
        )
      } ?: emptyList()

      videoKey to mapOf(
        "video_uid" to vid,
        "video_title" to (videoTitles[vid] ?: "Unknown"),
        "questions" to videoQuestions,
        "drills" to drills
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

  private fun showLoading() {
    loadingOverlay.visibility = View.VISIBLE
  }

  private fun hideLoading() {
    loadingOverlay.visibility = View.GONE
  }
}