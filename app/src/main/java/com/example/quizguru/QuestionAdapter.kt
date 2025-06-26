package com.example.quizguru

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class QuestionAdapter(
  private val questionList: List<Question>,
  private val onItemClick: (Question) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

  class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvQuestion: TextView = itemView.findViewById(R.id.tvShowQuestion)
    val tvTheme: TextView = itemView.findViewById(R.id.tvShowTheme)
    val tvDifficulty: TextView = itemView.findViewById(R.id.tvShowDifficulty)
    val tvType: TextView = itemView.findViewById(R.id.tvShowType)

  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_question, parent, false)
    return QuestionViewHolder(view)
  }

  override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
    val q = questionList[position]
    holder.tvQuestion.text = q.question
    holder.tvTheme.text = "Theme: ${q.theme}"
    holder.tvDifficulty.text = "Difficulty: ${q.difficulty.replaceFirstChar { it.uppercase(Locale.getDefault()) }}"
    holder.tvType.text = "Type: ${q.question_type?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Unknown"}"

    // for updating and deletion
    holder.itemView.setOnClickListener {
      onItemClick(q)
    }
  }

  override fun getItemCount(): Int = questionList.size
}
