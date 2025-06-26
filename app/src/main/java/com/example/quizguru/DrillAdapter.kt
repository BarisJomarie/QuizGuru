package com.example.quizguru

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quizguru.model.DrillQuestion

class DrillQuestionAdapter(
  private val drillList: List<DrillQuestion>,
  private val onItemClick: (DrillQuestion) -> Unit
) : RecyclerView.Adapter<DrillQuestionAdapter.DrillViewHolder>() {

  class DrillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvQuestion: TextView = itemView.findViewById(R.id.tvShowQuestion)
    val tvTheme: TextView = itemView.findViewById(R.id.tvShowTheme)
    val tvDifficulty: TextView = itemView.findViewById(R.id.tvShowDifficulty)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrillViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_question, parent, false) // Reuse layout if same
    return DrillViewHolder(view)
  }

  override fun onBindViewHolder(holder: DrillViewHolder, position: Int) {
    val dq = drillList[position]
    holder.tvQuestion.text = dq.question
    holder.tvTheme.text = "Theme: ${dq.theme}"
    holder.tvDifficulty.text = "Difficulty: ${dq.difficulty.replaceFirstChar { it.uppercase() }}"

    holder.itemView.setOnClickListener {
      onItemClick(dq)
    }
  }

  override fun getItemCount(): Int = drillList.size
}
