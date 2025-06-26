package com.example.quizguru

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.capitalize
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.w3c.dom.Text
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdminHomepageActivity : AppCompatActivity() {

  private lateinit var add_q: Button
  private lateinit var add_v: Button
  private lateinit var show_v: Button
  private lateinit var manage_users: Button

  private lateinit var play: Button
  private lateinit var logout: Button

  private lateinit var spThemeSelector: Spinner

  private lateinit var bcPreVsPost: BarChart
  private lateinit var tvThemeChartDescription: TextView
  private lateinit var userMap: Map<String, String>
  private lateinit var tvThemeChartErr: TextView

  private lateinit var bcDrillsChart: BarChart
  private lateinit var tvDrillChartDescription: TextView
  private lateinit var tvDrillChartErr: TextView

  private lateinit var pcResultChart: PieChart
  private lateinit var tvResultChartDescription: TextView
  private lateinit var tvResultChartErr: TextView

  private lateinit var tvChartSummary: TextView

  private lateinit var fAuth: FirebaseAuth
  private lateinit var fStore: FirebaseFirestore

  private var improvedCount = 0
  private var sameCount = 0
  private var declinedCount = 0
  private var maxImprover: String? = null
  private var maxDiff = 0
  private var maxDrillLabel: String = ""
  private var maxVideoTitle: String = ""
  private var maxDrillAttempts = 0

  private var currentTypingSession = 0
  private var typingJob: Job? = null



  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_admin_home)

    //Initialize firebase instances
    fAuth = FirebaseAuth.getInstance()
    fStore = FirebaseFirestore.getInstance()

    add_q = findViewById(R.id.btnAddQuestion)
    add_v = findViewById(R.id.btnAddVideo)
    show_v = findViewById(R.id.btnShowVideos)
    //eto yung dinagdag ko sir
    manage_users = findViewById(R.id.btnManageUsers)

    play = findViewById(R.id.btnPlay)
    logout = findViewById(R.id.btnLogout)

    spThemeSelector = findViewById(R.id.spThemeSelect)

    bcPreVsPost = findViewById(R.id.bcPreVsPost)
    tvThemeChartDescription = findViewById(R.id.tvThemeChartDescription)
    tvThemeChartErr = findViewById(R.id.tvThemeChartErr)

    bcDrillsChart = findViewById(R.id.bcDrillsChart)
    tvDrillChartDescription = findViewById(R.id.tvDrillChartDescription)
    tvDrillChartErr = findViewById(R.id.tvDrillChartErr)

    pcResultChart = findViewById(R.id.pcResultChart)
    tvResultChartDescription = findViewById(R.id.tvResultChartDescription)
    tvResultChartErr = findViewById(R.id.tvResultChartErr)

    tvChartSummary = findViewById(R.id.tvChartSummary)
    Log.d("Debug", "tvChartSummary is bound -> ${tvChartSummary != null}")


    fetchUsers()

    tvThemeChartErr.visibility = View.GONE
    tvDrillChartErr.visibility = View.GONE

    //theme spinner adapter
    val themes = listOf(
      "Crops & Plants", "Farm Animals", "Fruits & Vegetables", "Pests & Plants Diseases", "Soil & Fertilizers", "Tools & Machines"
    )
    val adapter = ArrayAdapter(
      this, android.R.layout.simple_spinner_item, themes
    ).also {
      it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
    spThemeSelector.adapter = adapter

    add_q.setOnClickListener {
      val intent = Intent(this, AddQuestionActivity::class.java)
      startActivity(intent)
    }

    add_v.setOnClickListener {
      val intent = Intent(this, AddVideoActivity::class.java)
      startActivity(intent)
    }

    show_v.setOnClickListener {
      val intent = Intent(this, ViewVideosActivity::class.java)
      startActivity(intent)
    }

    //eto yung dinagdag ko sir
    manage_users.setOnClickListener {
      val intent = Intent(this, AdminUserManagementActivity::class.java)
      startActivity(intent)
    }

    play.setOnClickListener {
      val intent = Intent(this, UserHomepageActivity::class.java)
      startActivity(intent)
      finish()
    }


    logout.setOnClickListener {
      AlertDialog.Builder(this)
        .setTitle("Logout")
        .setMessage("Are you sure you want to logout?")
        .setPositiveButton("Yes") {_, _ ->
          logout()
        }
        .setNegativeButton("Cancel", null)
        .show()
    }
  }

  private fun logout() {
    fAuth.signOut()
    val intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
    finish()
  }

  private fun fetchUsers() {
    fStore.collection("users")
      .get()
      .addOnSuccessListener { userSnap ->
        userMap = userSnap.documents.associate { doc ->
          val uid = doc.id
          val name = doc.getString("username") ?: uid.take(6)
          uid to name
        }

        spThemeSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
          override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val selectedTheme = parent?.getItemAtPosition(position).toString()
            fetchAttemptHistory(selectedTheme, userMap)
            Log.d("TYPE_DEBUG", "Spinner selected theme: $selectedTheme")

          }

          override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val selectedTheme = spThemeSelector.selectedItem.toString()
        fetchAttemptHistory(selectedTheme, userMap)
      }
  }

  private fun fetchAttemptHistory(selectedTheme: String, userMap: Map<String, String>) {
    tvChartSummary.text = ""
    tvChartSummary.alpha = 0f
    tvChartSummary.postDelayed({
      tvChartSummary.animate().alpha(1f).setDuration(500).start()
    }, 250)
    fStore.collection("attempt_history")
      .whereEqualTo("theme", selectedTheme)
      .get()
      .addOnSuccessListener { attemptSnap ->
        val docs = attemptSnap.documents
        tvChartSummary.alpha = 1f

        if (docs.isEmpty()) {
          noData(bcPreVsPost, tvThemeChartErr, tvThemeChartDescription, false)
          noData(bcDrillsChart, tvDrillChartErr, tvDrillChartDescription, false)
          noDataPie(pcResultChart, tvResultChartErr, tvResultChartDescription, false)

          val fallbackMessages = listOf(
            "Looks like we don't have enough data yet. Once users try this module, insights will appear here.",
            "No attempt data found. Encourage learners to take on this theme!",
            "Hang tight—this summary will light up once some quiz results come in."
          )
          val fallback = fallbackMessages.random()
          typeText(tvChartSummary, fallback)
          return@addOnSuccessListener
        }

        noData(bcPreVsPost, tvThemeChartErr, tvThemeChartDescription, true)
        noData(bcDrillsChart, tvDrillChartErr, tvDrillChartDescription, true)
        noDataPie(pcResultChart, tvResultChartErr, tvResultChartDescription, true)

        preTestAndPostTest(docs, userMap)
        drillAttemptChart(docs, selectedTheme)
        val attemptMaps = docs.mapNotNull { it.data }
        userResult(attemptMaps)
        val totalUsers = improvedCount + sameCount + declinedCount

        Log.d("TYPE_DEBUG", "Launching chartSummary for theme: $selectedTheme")

        chartSummary(
          theme = selectedTheme,
          improved = improvedCount,
          same = sameCount,
          declined = declinedCount,
          totalUsers = totalUsers,
          maxImprover = maxImprover,
          maxDiff = maxDiff,
          maxDrillLabel = maxDrillLabel,
          maxVideoTitle = maxVideoTitle,
          maxDrillAttempt = maxDrillAttempts
        )

      }
      .addOnFailureListener { e ->
        Log.e("Dashboard", "Fetch failed", e)
      }
  }

  private fun preTestAndPostTest(docs: List<DocumentSnapshot>, userMap: Map<String, String>) {
    bcPreVsPost.fitScreen()
    bcDrillsChart.fitScreen()
    // a) extract map of user → (pre, post)
    val prePostMap = extractPrePostScores(docs)

    //this is for description
    var improving = 0
    var same = 0
    var declining = 0

    prePostMap.values.forEach { (pre, post) ->
      if (pre != null && post != null) {
        when {
          post > pre -> improving++
          post == pre -> same++
          post < pre -> declining++
        }
      }
    }

    //for top improver
    var maxDifference = Int.MIN_VALUE
    val topUsers = mutableListOf<String>()
    prePostMap.forEach { (userId, scores) ->
      val (pre, post) = scores
      if (pre != null && post != null) {
        val diff = post - pre
        if (diff > maxDifference) {
          maxDifference = diff
          topUsers.clear()
          topUsers.add(userMap[userId] ?: "Unknown User")
        } else if (diff == maxDifference) {
          topUsers.add(userMap[userId] ?: "Unknown User")
        }
      }
    }

    improvedCount = improving
    sameCount = same
    declinedCount = declining
    maxDiff = maxDifference
    maxImprover = topUsers.firstOrNull()



    val message = when {
      improving > same && improving > declining -> "Most users in this theme are improving. This suggests that the drills are effective."
      declining > same && declining > improving -> "Most users in this theme performed worse after the drill. You may need to adjust the content or difficulty."
      same >= improving && same >= declining -> "Most users performed the same before and after the drill. This may mean the drill is too neutral or easy."
      else -> "Mixed results. No clear trend among users for this theme."
    }
    val topUserNote = when {
      maxDifference <= 0 -> "\nCurrently everyone stayed the same after taking the quizzes and drills."
      topUsers.size == 1 -> "\nThe top improver currently is ${topUsers.first()} with a $maxDifference-point increase."
      else -> "\nThere are ${topUsers.size} users who share the highest improvement of $maxDifference points."
    }

    tvThemeChartDescription.text = message + topUserNote

    // b) build entries + labels
    val preEntries  = ArrayList<BarEntry>()
    val postEntries = ArrayList<BarEntry>()
    val labels      = ArrayList<String>()
    prePostMap.entries.forEachIndexed { idx, (uid, scores) ->
      val name = userMap[uid] ?: uid.take(6)
      labels.add(name)
      preEntries .add(BarEntry(idx.toFloat(), scores.first?.toFloat()  ?: 0f))
      postEntries.add(BarEntry(idx.toFloat(), scores.second?.toFloat() ?: 0f))
    }

    // c) create DataSets
    val preSet = BarDataSet(preEntries, "Pre-Test").apply {
      color = ContextCompat.getColor(this@AdminHomepageActivity, R.color.purple_200)
      setDrawValues(true)
    }
    val postSet = BarDataSet(postEntries, "Post-Test").apply {
      color = ContextCompat.getColor(this@AdminHomepageActivity, R.color.purple_500)
      setDrawValues(true)
    }

    // d) attach to chart
    bcPreVsPost.data = BarData(preSet, postSet).apply { barWidth = 0.3f }
    with(bcPreVsPost.xAxis) {
      valueFormatter       = IndexAxisValueFormatter(labels)
      granularity          = 1f
      setCenterAxisLabels(true)
      position             = XAxis.XAxisPosition.BOTTOM
    }
    bcPreVsPost.axisLeft.axisMinimum = 0f
    bcPreVsPost.axisRight.apply {
      isEnabled       = true
      setDrawGridLines(false)
      setDrawLabels(false)
      setDrawAxisLine(true)
      axisMinimum     = bcPreVsPost.axisLeft.axisMinimum
      axisMaximum     = bcPreVsPost.axisLeft.axisMaximum
    }

    bcPreVsPost.groupBars(0f, 0.4f, 0.05f)
    bcPreVsPost.apply {
      description.isEnabled = false
      legend.apply {
        verticalAlignment   = Legend.LegendVerticalAlignment.TOP
        horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        orientation         = Legend.LegendOrientation.HORIZONTAL
        setDrawInside(false)
      }

      val lastIndex = labels.size.toFloat()
      xAxis.axisMinimum = 0f
      xAxis.axisMaximum = lastIndex + 0.5f

      setExtraOffsets(0f, 0f, 16f, 0f)
      animateY(600)
      invalidate()
    }
  }

  private fun extractPrePostScores(docs: List<DocumentSnapshot>): Map<String, Pair<Int?, Int?>> {
    val result = mutableMapOf<String, Pair<Int?, Int?>>()

    docs.forEach { doc ->
      val uid = doc.getString("user_id") ?: return@forEach
      val type = doc.getString("attempt_type") ?: return@forEach
      val score = doc.getLong("score")?.toInt()

      val (existingPre, existingPost) = result[uid] ?: (null to null)
      result[uid] = when (type) {
        "pre_test" -> (score to existingPost)
        "post_test" -> (existingPre to score)
        else -> existingPre to existingPost
      }
    }

    return result
  }

  private fun drillAttemptChart(docs: List<DocumentSnapshot>, selectedTheme: String) {
    val videoDrillMap = mutableMapOf<String, List<Int>>()
    val allDrillsCount = mutableSetOf<Int>()

    //aggregate attempts
    docs.filter {
      it.getString("attempt_type") == "post_test" &&
              it.getString("theme") == selectedTheme
    }
      .forEach { doc ->
        val videoBreakdown = doc.get("video_breakdown") as? Map<*, *> ?: return@forEach

        videoBreakdown.forEach { (_, videoDataAny) ->
          val videoData = videoDataAny as? Map<*, *> ?: return@forEach
          val videoTitle = videoData["video_title"] as? String ?: "Untitled Video"
          val drills = videoData["drills"] as? List<*> ?: return@forEach

          val attempts = drills.mapIndexed { index, drillAny ->
            allDrillsCount.add(index)
            val drill = drillAny as? Map<*, *> ?: return@mapIndexed 0
            (drill["attempt_count"] as? Long)?.toInt() ?: 0

          }

          val paddedAttempts = attempts + List(allDrillsCount.size - attempts.size) { 0 }
          val currentList = videoDrillMap[videoTitle] ?: List(allDrillsCount.size) { 0 }
          videoDrillMap[videoTitle] = currentList.zip(paddedAttempts) { a, b -> maxOf(a, b) }

          val maxAttempt = paddedAttempts.maxOrNull() ?: 0
          val maxIndex = paddedAttempts.indexOf(maxAttempt)

          if (maxAttempt > maxDrillAttempts) {
            maxDrillAttempts = maxAttempt
            maxDrillLabel = "Drill ${maxIndex + 1}"
            maxVideoTitle = videoTitle
          }
        }
      }
    if (videoDrillMap.isEmpty()) {
      Toast.makeText(this, "No drill attempt data available", Toast.LENGTH_SHORT).show()
      return
    }

    //Build entries
    val barDataSets = ArrayList<BarDataSet>()
    val colors = listOf(
      R.color.purple_200,
      R.color.purple_500,
      R.color.teal_200,
      R.color.teal_700,
      R.color.black
    )

    val drillLabels = allDrillsCount.sorted().map { "Drill ${it+1}" }
    videoDrillMap.entries.forEachIndexed { vidIdx, (videoTitle, attempts) ->
      val entries = ArrayList<BarEntry>()
      attempts.forEachIndexed { drillIdx, count ->
        entries.add(BarEntry(drillIdx.toFloat(), count.toFloat()))
      }

      val shortTitle = if (videoTitle.length > 8) {
        videoTitle.take(8) + "..."
      } else {
        videoTitle
      }
      val set = BarDataSet(entries, shortTitle)
      set.color = ContextCompat.getColor(this, colors[vidIdx % colors.size])
      barDataSets.add(set)
    }

    bcDrillsChart.data = BarData(barDataSets as List<IBarDataSet>).apply {
      barWidth = 0.25f
    }

    bcDrillsChart.xAxis.apply {
      valueFormatter = IndexAxisValueFormatter(drillLabels)
      granularity = 1f
      setCenterAxisLabels(true)
      position = XAxis.XAxisPosition.BOTTOM
      isGranularityEnabled = true
    }

    val groupCount = drillLabels.size
    val groupSpace = 0.2f
    val barSpace = 0.02f
    val barWidth = 0.25f
    bcDrillsChart.data.barWidth = barWidth
    bcDrillsChart.groupBars(0f, groupSpace, barSpace)
    bcDrillsChart.legend.apply {
      verticalAlignment = Legend.LegendVerticalAlignment.TOP
      horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
      orientation = Legend.LegendOrientation.HORIZONTAL
      setDrawInside(false)

    }
    bcDrillsChart.xAxis.axisMinimum = 0f
    val groupWidth = bcDrillsChart.barData.getGroupWidth(groupSpace, barSpace)
    bcDrillsChart.xAxis.axisMaximum = groupWidth * groupCount
    bcDrillsChart.axisLeft.axisMinimum = 0f
    bcDrillsChart.axisRight.isEnabled = false
    bcDrillsChart.description.isEnabled = false
    bcDrillsChart.legend.isEnabled = true
    bcDrillsChart.setExtraOffsets(0f, 0f, 24f, 10f)
    bcDrillsChart.animateY(600)
    bcDrillsChart.invalidate()

    var maxAttempt = 0
    var maxDrillIndex = -1
    var maxVideoTitle = ""

    videoDrillMap.forEach { (videoTitle, attempts) ->
      attempts.forEachIndexed { index, count ->
        if (count > maxAttempt) {
          maxAttempt = count
          maxDrillIndex = index
          maxVideoTitle = videoTitle
        }
      }
    }

    val totalDrills = drillLabels.size
    val maxDrillLabel = if (maxDrillIndex in drillLabels.indices) drillLabels[maxDrillIndex] else "Unknown Drill"

    val description = "This chart visualizes the highest number of attempts made on $totalDrills drills. " +
      "The peak was $maxAttempt attempts on $maxDrillLabel from the video \"${maxVideoTitle}\"."

    tvDrillChartDescription.text = description
  }

  private fun userResult(attempts: List<Map<String, Any>>) {
    val userStatusMap = mutableMapOf<String, String>()

    attempts.forEach { entry ->
      val userId = entry["user_id"] as? String ?: return@forEach
      val result = entry["result"] as? Map<*, *> ?: return@forEach
      val status = result["status"] as? String ?: return@forEach
      userStatusMap[userId] = status
    }

    val resultCounts = mapOf(
      "improved" to userStatusMap.values.count { it == "improved" },
      "same" to userStatusMap.values.count { it == "same" },
      "declined" to userStatusMap.values.count { it == "declined" }
    )

    val entries = ArrayList<PieEntry>()
    resultCounts.forEach { (label, count) ->
      if (count > 0) entries.add(PieEntry(count.toFloat(), label.capitalize()))
    }

    val dataset = PieDataSet(entries, "User Outcomes").apply {
      colors = listOf(
        ContextCompat.getColor(this@AdminHomepageActivity, R.color.teal_700),
        ContextCompat.getColor(this@AdminHomepageActivity, R.color.purple_200),
        ContextCompat.getColor(this@AdminHomepageActivity, R.color.light_coral)
      )
      valueTextSize = 14f
      sliceSpace = 2f
    }

    val pieData = PieData(dataset)

    pcResultChart.apply {
      data = pieData
      centerText = "User Status"
      setUsePercentValues(true)
      isDrawHoleEnabled = true
      holeRadius = 40f
      transparentCircleRadius = 45f
      description.isEnabled = false
      legend.isEnabled = true
      animateY(600)
      invalidate()
    }

    val improved = resultCounts["improved"] ?: 0
    val same = resultCounts["same"] ?: 0
    val declined = resultCounts["declined"] ?: 0
    val total = improved + same + declined

    val summary = when {
      total == 0 -> "No user data available for this theme. The pie chart will appear once at least one user completes a valid post-test attempt."
      improved > same && improved > declined -> "$improved out of $total users showed improvement after the drill. This suggests that the content or activities helped enhance their understanding."
      declined > same && declined > improved -> "$declined out of $total users performed worse on the post-test than the pre-test. You might want to revisit this theme’s material or check if the difficulty is too high."
      same > improved && same > declined -> "$same out of $total users had no change in score. This could mean that the drill didn't strongly impact learning outcomes, or the material may need more challenge or engagement."
      else -> "Users were quite evenly split: $improved improved, $same stayed the same, and $declined declined. The results show no dominant trend, so the theme may be producing mixed effects depending on user background."
    }

    tvResultChartDescription.text = summary
  }

  private fun chartSummary(
    theme: String,
    improved: Int,
    same: Int,
    declined: Int,
    totalUsers: Int,
    maxImprover: String?,
    maxDiff: Int,
    maxDrillLabel: String,
    maxVideoTitle: String,
    maxDrillAttempt: Int
  ) {
    Log.d("TYPE_DEBUG", """
    chartSummary called:
    Theme: $theme
    Improved: $improved
    Same: $same
    Declined: $declined
    Max Improver: $maxImprover
    MaxDiff: $maxDiff
    DrillLabel: $maxDrillLabel
    VideoTitle: $maxVideoTitle
""".trimIndent())


    if (totalUsers == 0) {
      val fallback = "No attempt data is currently available for the \"$theme\" module. Once users complete the drills and post-tests, a performance summary will be generated here."
      typeText(tvChartSummary, fallback)
      return
    }

    val topImproverNote = if (maxImprover != null && maxDiff > 0) {
      "The most improved user is $maxImprover, gaining $maxDiff points between pre- and post-test."
    } else {
      "Currently, no users stand out with notable score improvement."
    }

    val performanceTrend = when {
      improved > same && improved > declined ->
        "$improved out of $totalUsers users had lower pre-test scores and showed improvement after taking the drill. This highlights the drill's impact on their post-test results."

      declined > same && declined > improved ->
        "$declined users showed a decline in performance. This could indicate that the theme or content may require revision or clarity improvements."

      same >= improved && same >= declined ->
        "$same users maintained the same score before and after the drill. This might suggest that the drill wasn't challenging enough to influence outcomes."

      else ->
        "User results are varied across this theme, with no clearly dominant trend in performance."
    }

    val drillInsight =
      "In terms of engagement, the most attempted drill was \"$maxDrillLabel\" from the video \"$maxVideoTitle\", reaching up to $maxDrillAttempt attempts by users. This may indicate high complexity or strong user motivation."

    val pieInsight =
      "According to their latest outcomes: $improved users improved, $same remained the same, and $declined declined. This provides a snapshot of overall user performance."

    val summary = "Across users who completed the \"$theme\" module:\n\n" +
      "$topImproverNote\n\n" +
      "To assess improvement trends: $performanceTrend\n\n" +
      "Engagement-wise, mini drills were included to reinforce learning. $drillInsight\n\n" +
      "Lastly, $pieInsight"

    Log.d("TYPE_DEBUG", "Typing summary for theme: $theme")

    typeText(tvChartSummary, summary)
  }


  private fun noDataPie(selectedChart: PieChart, selectedErr: TextView, selectedDescription: TextView, data: Boolean) {
    if (data) {
      selectedChart.visibility = View.VISIBLE
      selectedDescription.visibility = View.VISIBLE
      selectedErr.visibility = View.GONE
    } else {
      selectedChart.visibility = View.GONE
      selectedDescription.visibility = View.GONE
      selectedErr.visibility = View.VISIBLE
    }
  }


  private fun noData(selectedChart: BarChart, selectedErr: TextView, selectedDescription: TextView, data: Boolean) {
    if (data) {
      selectedChart.visibility = View.VISIBLE
      selectedDescription.visibility = View.VISIBLE
      selectedErr.visibility = View.GONE
    } else {
      selectedChart.visibility = View.GONE
      selectedDescription.visibility = View.GONE
      selectedErr.visibility = View.VISIBLE
    }
  }

  //animation lang to hahahha
  private val handler = Handler(Looper.getMainLooper())
  private var typeRunnable: Runnable? = null

  private fun typeText(textView: TextView, fullText: String, delay: Long = 10L) {

    // Cancel any previous animation
    typingJob?.cancel()

    textView.text = ""

    typingJob = CoroutineScope(Dispatchers.Main).launch {
      for (i in 0..fullText.length) {
        textView.text = fullText.substring(0, i)
        delay(delay)
      }
    }
  }



}