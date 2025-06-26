package com.example.quizguru

data class DrillAnswer(
  val question: String = "",
  var attemptCount: Int = 0,
  var finalAnswer: List<String> = listOf(),
  var isCorrect: Boolean = false,
  var correctAnswer: String = ""
)
