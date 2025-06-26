package com.example.quizguru
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Question model
@Parcelize
data class Question(
  var id: String? = null, // Firestore question ID
  val theme: String = "", // Question theme
  val question: String = "", // Question question
  val options: Map<String, String> = mapOf(), // Question options A, B, C, D
  val answer: String = "", // Question answer
  val difficulty: String = "", // Question difficulty
  val facts: String = "", // Question facts
  val references: String = "", // Facts references
  var video_uid: String? = null, // videoId FK
  var question_type: String? = null, // ype of question(multiple choice)
  var type: String? = null, // type (test or drills)
  var uploaded_by: String? = null,
  var created_at: com.google.firebase.Timestamp? = null,
  var updated_at: com.google.firebase.Timestamp? = null,

  //save chosen answer letter
  var selectedAnswer: String? = null
) : Parcelable