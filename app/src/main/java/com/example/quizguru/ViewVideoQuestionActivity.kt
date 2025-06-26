package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizguru.model.DrillQuestion
import com.google.firebase.firestore.FirebaseFirestore


class ViewVideoQuestionActivity : AppCompatActivity() {

  private lateinit var back: Button
  private lateinit var add: Button
  private lateinit var showTest: Button
  private lateinit var showDrill: Button

  private lateinit var viewQuestions: RecyclerView
  private lateinit var questionAdapter: QuestionAdapter
  private lateinit var questionList: MutableList<Question>

  private lateinit var viewDrill: RecyclerView
  private lateinit var drillAdapter: DrillQuestionAdapter
  private lateinit var drillList: MutableList<DrillQuestion>

  private lateinit var videoName: TextView
  private lateinit var loadingOverlay: LinearLayout
  private lateinit var noQuestionFetchText: TextView

  private lateinit var fStore: FirebaseFirestore

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_video_questions)

    fStore = FirebaseFirestore.getInstance()

    back = findViewById(R.id.btnBack)
    add = findViewById(R.id.btnAdd)
    showTest = findViewById(R.id.btnTestQuestion)
    showDrill = findViewById(R.id.btnDrillQuestion)

    viewQuestions = findViewById(R.id.rvViewQuestions)
    viewQuestions.layoutManager = LinearLayoutManager(this)
    questionList = mutableListOf()
    questionAdapter = QuestionAdapter(questionList) { selectedQuestion ->
      val intent = Intent(this, EditDeleteQuestionActivity::class.java)
      intent.putExtra("questionId", selectedQuestion.id)
      intent.putExtra("theme", selectedQuestion.theme)
      intent.putExtra("questionText", selectedQuestion.question)
      intent.putExtra("options", HashMap(selectedQuestion.options))
      intent.putExtra("correctAnswer", selectedQuestion.answer)
      intent.putExtra("difficulty", selectedQuestion.difficulty)
      intent.putExtra("type", selectedQuestion.type)
      startActivity(intent)
    }
    viewQuestions.adapter = questionAdapter

    viewDrill = findViewById(R.id.rvViewDrill)
    viewDrill.layoutManager = LinearLayoutManager(this)
    drillList = mutableListOf()
    drillAdapter = DrillQuestionAdapter(drillList) { selectedQuestion ->
      val intent = Intent(this, EditDeleteDrillActivity::class.java)
      intent.putExtra("question_id", selectedQuestion.id)
      intent.putExtra("theme", selectedQuestion.theme)
      intent.putExtra("question", selectedQuestion.question)
      intent.putStringArrayListExtra("options", ArrayList(selectedQuestion.options))
      intent.putIntegerArrayListExtra("correct_indexes", ArrayList(selectedQuestion.correct_indexes))
      intent.putExtra("difficulty", selectedQuestion.difficulty)
      intent.putExtra("type", selectedQuestion.type)
      startActivity(intent)
    }
    viewDrill.adapter = drillAdapter




    videoName = findViewById(R.id.tvVideoName)
    loadingOverlay = findViewById(R.id.loadingOverlay)
    noQuestionFetchText = findViewById(R.id.tvNoQuestionFetched)

    val videoUid = intent.getStringExtra("video_id") ?: ""

    hideQuestionFetchText()
    fetchQuestion1(videoUid)

    add.setOnClickListener {
      val intent = Intent(this, AddQuestionActivity::class.java)
      intent.putExtra("video_uid", videoUid)
      startActivity(intent)
    }

    back.setOnClickListener {
      finish()
    }

    showTest.setOnClickListener {
      fetchQuestion1(videoUid)
      viewDrill.visibility = View.GONE
      viewQuestions.visibility = View.VISIBLE
    }

    showDrill.setOnClickListener {
      fetchQuestion2(videoUid)
      viewQuestions.visibility = View.GONE
      viewDrill.visibility = View.VISIBLE
    }

  }

  private fun showQuestionFetchText() {
    noQuestionFetchText.visibility = View.VISIBLE
  }

  private fun hideQuestionFetchText() {
    noQuestionFetchText.visibility = View.GONE
  }

  private fun showLoading() {
    loadingOverlay.visibility = View.VISIBLE
  }

  private fun hideLoading() {
    loadingOverlay.visibility = View.GONE
  }

  private fun fetchQuestion1(videoUid: String) {
    showLoading()
    fStore.collection("questions")
      .whereEqualTo("video_uid", videoUid)
      .whereEqualTo("type", "test")
      .get()
      .addOnSuccessListener { result ->
        questionList.clear()
        hideLoading()
        for (doc in result) {
          val question = doc.toObject(Question::class.java)
          question.id = doc.id
          questionList.add(question)
        }
        questionAdapter.notifyDataSetChanged()
        if (questionList.isEmpty())
          showQuestionFetchText()
      }
  }

  private fun fetchQuestion2(videoUid: String) {
    showLoading()
    fStore.collection("questions")
      .whereEqualTo("video_uid", videoUid)
      .whereEqualTo("type", "drill")
      .get()
      .addOnSuccessListener { result ->
        drillList.clear()
        hideLoading()
        for (doc in result) {
          val question = doc.toObject(DrillQuestion::class.java)
          question.id = doc.id
          drillList.add(question)
        }
        drillAdapter.notifyDataSetChanged()
        if (drillList.isEmpty())
          showQuestionFetchText()
      }
  }
}