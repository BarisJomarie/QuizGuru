package com.example.quizguru


data class Video(
  var id: String? = null,
  val title: String = "",
  val description: String = "",
  val url: String = "",
  val theme: String = "",
  val order: Int = 0,
  val created_at: com.google.firebase.Timestamp? = null,
  var uploaded_by: String? = null
)