import android.view.DragEvent
import android.view.View
import android.widget.TextView
import com.example.quizguru.R

class DrillDragListener : View.OnDragListener {
  override fun onDrag(v: View, event: DragEvent): Boolean {
    when (event.action) {
      DragEvent.ACTION_DROP -> {
        val draggedText = event.clipData.getItemAt(0).text
        (v as TextView).text = draggedText
        v.background = v.context.getDrawable(R.drawable.drop_slot_filled)
      }
    }
    return true
  }
}
