package com.example.quizguru

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainGameResultActivity : AppCompatActivity() {

  private lateinit var tvScoreComparison: TextView
  private lateinit var tvDifficulty: TextView
  private lateinit var tvTheme: TextView
  private lateinit var tvPreTestScore: TextView
  private lateinit var tvPostTestScore: TextView
  private lateinit var tvPerformanceScore: TextView
  private lateinit var tvDescription: TextView
  private lateinit var videoBreakdown1: LinearLayout
  private lateinit var videoBreakdown2: LinearLayout
  private lateinit var videoBreakdown3: LinearLayout
  private lateinit var tvVideo1: TextView
  private lateinit var tvVideo2: TextView
  private lateinit var tvVideo3: TextView
  private lateinit var btnFinish: Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_result)

    tvScoreComparison = findViewById(R.id.tvComparison)
    tvDifficulty = findViewById(R.id.tvDifficulty)
    tvTheme = findViewById(R.id.tvTheme)
    tvPreTestScore = findViewById(R.id.tvPreTestScore)
    tvPostTestScore = findViewById(R.id.tvPostTestScore)
    tvPerformanceScore = findViewById(R.id.tvPerformanceScore)
    tvDescription = findViewById(R.id.tvPerformanceDescription)
    videoBreakdown1 = findViewById(R.id.video1Breakdown)
    videoBreakdown2 = findViewById(R.id.video2Breakdown)
    videoBreakdown3 = findViewById(R.id.video3Breakdown)
    tvVideo1 = findViewById(R.id.tvVideo1)
    tvVideo2 = findViewById(R.id.tvVideo2)
    tvVideo3 = findViewById(R.id.tvVideo3)

    btnFinish = findViewById(R.id.btnFinish)

    val postTestResults = intent.getParcelableArrayListExtra<Question>("post_test_results") ?: arrayListOf()
    val difficulty = intent.getStringExtra("difficulty") ?: ""
    val theme = intent.getStringExtra("theme") ?: ""
    val videoIds = intent.getStringArrayListExtra("video_ids") ?: arrayListOf()
    val videoTitles = intent.getSerializableExtra("video_titles") as? HashMap<String, String> ?: hashMapOf()

    val preScore = PrePostManager.getPreTestScore(this)
    val postScore = PrePostManager.getPostTestScore(this)

    tvDifficulty.text = "${difficulty.replaceFirstChar { it.uppercase() }}"
    tvTheme.text = "${theme}"


    //Show score
    val totalQuestions = postTestResults.size.takeIf { it > 0 } ?: 1
    tvPreTestScore.text = "$preScore / ${totalQuestions}"
    tvPostTestScore.text = "$postScore / ${totalQuestions}"

    //pre and post percentage
    val prePercentage = (preScore.toFloat() / totalQuestions) * 100
    val postPercentage = (postScore.toFloat() / totalQuestions) * 100
    val averagePercentage = (prePercentage + postPercentage) / 2

    val description = when {
      averagePercentage >= 90 -> "Outstanding performance"
      averagePercentage >= 75 -> "Great job!"
      averagePercentage >= 50 -> "Needs improvement"
      averagePercentage >= 20 -> "Weak performance"
      else -> "Requires serious attention"
    }

    tvPerformanceScore.text = String.format("%.1f%%", averagePercentage)
    tvDescription.text = description

    //Show performance
    val status = when {
      postScore > preScore -> "Improved!"
      postScore < preScore -> "Declined!"
      else -> "No Change"
    }
    tvScoreComparison.text = "Performance: $status"

    //Breakdown of videos
    val layoutMap = listOf(videoBreakdown1, videoBreakdown2, videoBreakdown3)
    val orderedVideos = videoTitles.keys.toList()
    val videoTitleViews = listOf(tvVideo1, tvVideo2, tvVideo3)

    for (i in orderedVideos.indices) {
      val videoId = orderedVideos[i]
      val title = videoTitles[videoId] ?: "Untitled Video"
      videoTitleViews.getOrNull(i)?.text = "Video: ${title}"
    }

    val preTestMap = PrePostManager.getPreTestQuestions().groupBy { it.video_uid }
    val postTestMap = PrePostManager.getPostTestQuestions().groupBy { it.video_uid }
    val drillMap = PrePostManager.getDrillAnswers()

    for ((index, videoId) in orderedVideos.take(3).withIndex()) {
      val layout = layoutMap.getOrNull(index) ?: continue

      //preTest
      val pretestHeader = TextView(this).apply {
        text = "Pre-test Results"
        setTypeface(null, Typeface.BOLD)
        textSize = 16f
        setPadding(8, 16, 8, 4)
      }
      layout.addView(pretestHeader)
      preTestMap[videoId]?.forEachIndexed { i, q ->
        val isCorrect = q.selectedAnswer?.equals(q.answer, ignoreCase = true) == true
        val view = TextView(this).apply {
          text = "Pre Q${i + 1}: ${q.question}\nYour Answer: ${q.selectedAnswer}\nCorrect Answer: ${q.answer}\nResult: ${if (isCorrect) "Correct" else "Wrong"}"
          textSize = 15f
          setPadding(8, 4, 8, 8)
        }
        view.setTextColor(
          ContextCompat.getColor(
            this,
            if (isCorrect) R.color.light_green else R.color.red
          )
        )
        layout.addView(view)
      }

      //drills
      val drillHeader = TextView(this).apply {
        text = "Drills Results"
        setTypeface(null, Typeface.BOLD)
        textSize = 16f
        setPadding(8, 16, 8, 4)
      }
      layout.addView(drillHeader)
      drillMap[videoId]?.forEach {
        val drillView = TextView(this).apply {
          text = "${ it.question }\nAttempts: ${ it.attemptCount }\nCorrect: ${ if (it.isCorrect) "Correct" else "Wrong" }"
          textSize = 15f
          setPadding(8, 4, 8, 8)
        }
        drillView.setTextColor(
          ContextCompat.getColor(
            this,
            if (it.isCorrect) R.color.light_green else R.color.red
          )
        )
        layout.addView(drillView)
      }

      //postTest
      val posttestHeader = TextView(this).apply {
        text = "Post-Test Results"
        setTypeface(null, Typeface.BOLD)
        textSize = 16f
        setPadding(8, 16, 8, 4)
      }
      layout.addView(posttestHeader)
      postTestMap[videoId]?.forEachIndexed { i, q ->
        val isCorrect = q.selectedAnswer?.equals(q.answer, ignoreCase = true) == true
        val view = TextView(this).apply {
          text = "Post Q${i + 1}: ${q.question}\nYour Answer: ${q.selectedAnswer}\nCorrect Answer: ${q.answer}\nResult: ${if (isCorrect) "Correct" else "Wrong"}"
          textSize = 14f
          setPadding(8, 4, 8, 8)
        }
        view.setTextColor(
          ContextCompat.getColor(
            this,
            if (isCorrect) R.color.light_green else R.color.red
          )
        )
        layout.addView(view)
      }

      // just in case no question fetched
      if (preTestMap[videoId].isNullOrEmpty() && drillMap[videoId].isNullOrEmpty() && postTestMap[videoId].isNullOrEmpty()) {
        val emptyNote = TextView(this).apply {
          text = "No data recorded for this video."
          setTextColor(ContextCompat.getColor(this@MainGameResultActivity, R.color.gray))
          setPadding(8, 4, 8, 8)
        }
        layout.addView(emptyNote)
      }

    }

    btnFinish.setOnClickListener {
      PrePostManager.clear()
      val intent = Intent(this, UserHomepageActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      startActivity(intent)
    }
  }
}