package com.example.quizguru

import android.preference.PreferenceManager
import android.content.Context

object PrePostManager {

  private var preTestQuestions: List<Question>? = null
  private var postTestQuestions: List<Question>? = null
  private var drillAnswers: Map<String, List<DrillAnswer>> = emptyMap()

  fun savePreTestQuestions(questions: List<Question>) {
    preTestQuestions = questions
  }

  fun getPreTestQuestions(): List<Question> {
    return preTestQuestions ?: emptyList()
  }

  fun getPreTestScore(): Int {
    return preTestQuestions?.count() { it.selectedAnswer?.lowercase() == it.answer.lowercase() } ?: 0
  }

  fun getPreTestScore(context: Context): Int {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    return prefs.getInt("pre_test_score", 0)
  }

  fun savePreTestScore(score: Int, context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    prefs.edit().putInt("pre_test_score", score).apply()
  }


  fun savePostTestQuestions(questions: List<Question>) {
    postTestQuestions = questions
  }

  fun getPostTestQuestions(): List<Question> {
    return postTestQuestions ?: emptyList()
  }

  fun getPostTestScore(context: Context): Int {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    return prefs.getInt("post_test_score", 0)
  }

  fun savePostTestScore(score: Int, context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    prefs.edit().putInt("post_test_score", score).apply()
  }

  fun getPostTestScore(): Int {
    return postTestQuestions?.count() { it.selectedAnswer?.lowercase() == it.answer.lowercase() } ?: 0
  }


  fun setDrillAnswers(data: Map<String, List<DrillAnswer>>) {
    drillAnswers = data
  }

  fun getDrillAnswers(): Map<String, List<DrillAnswer>> {
    return drillAnswers
  }




  fun clear() {
    preTestQuestions = emptyList()
    postTestQuestions = emptyList()
    drillAnswers = emptyMap()
  }

}