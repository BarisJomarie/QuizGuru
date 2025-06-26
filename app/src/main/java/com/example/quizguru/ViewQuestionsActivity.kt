package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.ArrayList

class ViewQuestionsActivity : AppCompatActivity() {
  private lateinit var viewQuestion: RecyclerView
  private lateinit var questionList: MutableList<Question>
  private lateinit var adapter: QuestionAdapter
  private lateinit var back: Button
  private lateinit var theme: Spinner
  private lateinit var noQuestion: TextView
  private lateinit var loadingOverlay: LinearLayout

  private lateinit var fStore: FirebaseFirestore

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_view_question)

    fStore = FirebaseFirestore.getInstance()

    viewQuestion = findViewById(R.id.rvViewQuestions)
    viewQuestion.layoutManager = LinearLayoutManager(this)
    questionList = mutableListOf()
    adapter = QuestionAdapter(questionList) { selectedQuestion ->
      val intent = Intent(this, EditDeleteQuestionActivity::class.java)
      intent.putExtra("questionId", selectedQuestion.id)
      intent.putExtra("theme", selectedQuestion.theme)
      intent.putExtra("questionText", selectedQuestion.question)
      intent.putExtra("options", HashMap(selectedQuestion.options))
      intent.putExtra("correctAnswer", selectedQuestion.answer)
      intent.putExtra("difficulty", selectedQuestion.difficulty)
      startActivity(intent)
    }
    viewQuestion.adapter = adapter
    theme = findViewById(R.id.spTheme)
    back = findViewById(R.id.btnBack)
    noQuestion = findViewById(R.id.tvNoQuestion)
    loadingOverlay = findViewById(R.id.loadingOverlay)

    back.setOnClickListener {
      finish()
    }

    setupTheme()
    theme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val selectedTheme = parent?.getItemAtPosition(position).toString()
        if (selectedTheme == "All") {
          fetchQuestions()
        } else {
          fetchQuestionsByTheme(selectedTheme)
        }
      }

      override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
  }

  private fun setupTheme() {
    val themeList = mutableListOf<String>()
    themeList.add("All")

    fStore.collection("themes")
      .get()
      .addOnSuccessListener { result ->
        for (doc in result) {
          val themeName = doc.getString("name")
          if(themeName != null)
            themeList.add(themeName)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        theme.adapter = adapter
      }
      .addOnFailureListener {
        Toast.makeText(this, "Failed to load themes.", Toast.LENGTH_SHORT).show()
      }
  }



  //get all questions
  private fun fetchQuestions() {
    loadingVisible()
    fStore.collection("questions")
      .get()
      .addOnSuccessListener { result ->
        questionList.clear()
        noQuestion.visibility = View.GONE
        loadingGone()
        for(doc in result) {
          val question = doc.toObject(Question::class.java)
          question.id = doc.id
          questionList.add(question)
        }
        adapter.notifyDataSetChanged()
        if (questionList.isEmpty()) {
          noQuestion.visibility = View.VISIBLE
        }
      }
      .addOnFailureListener {
        loadingGone()
        Toast.makeText(this, "Failed to load questions.", Toast.LENGTH_SHORT).show()
      }
  }

  //get question based on theme selected
  private fun fetchQuestionsByTheme(selectedTheme: String) {
    loadingVisible()
    fStore.collection("questions")
      .whereEqualTo("theme", selectedTheme)
      .get()
      .addOnSuccessListener { result ->
        questionList.clear()
        noQuestion.visibility = View.GONE
        loadingGone()
        for(doc in result) {
          val question = doc.toObject(Question::class.java)
          question.id = doc.id
          questionList.add(question)
        }
        adapter.notifyDataSetChanged()
        if (questionList.isEmpty()) {
          noQuestion.visibility = View.VISIBLE
        }

      }
      .addOnFailureListener {
        loadingGone()
        Toast.makeText(this, "Failed to load questions.", Toast.LENGTH_SHORT).show()
      }
  }

  private fun loadingVisible() {
    loadingOverlay.visibility = View.VISIBLE
  }

  private fun loadingGone() {
    loadingOverlay.visibility = View.GONE
  }
}