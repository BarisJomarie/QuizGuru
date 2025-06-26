package com.example.quizguru.model

import com.google.firebase.Timestamp

data class DrillQuestion(
  var id: String? = null,
  val theme: String = "",
  val question: String = "",
  val options: List<String> = listOf(),
  val correct_indexes: List<Int> = listOf(),
  val difficulty: String = "",
  var video_uid: String? = null,
  var question_type: String? = null,
  var type: String? = null, // should be "drill"
  var uploaded_by: String? = null,
  var created_at: Timestamp? = null,
  var updated_at: Timestamp? = null
)
