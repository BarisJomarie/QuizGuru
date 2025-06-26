package com.example.quizguru

import android.net.Uri
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class VideoExample : AppCompatActivity() {

  private lateinit var youtubeWebView: WebView
  private lateinit var fStore: FirebaseFirestore

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.video_example)

    youtubeWebView = findViewById(R.id.youtubeWebView)
    fStore = FirebaseFirestore.getInstance()

    val selectedTheme = "Crops & Plants"
    val selectedDifficulty = "easy"

    fStore.collection("videos")
      .whereEqualTo("theme", selectedTheme)
      .whereEqualTo("difficulty", selectedDifficulty)
      .get()
      .addOnSuccessListener { snapshot ->
        if (!snapshot.isEmpty) {
          val doc = snapshot.documents[0]
          val ytUrl = doc.getString("video_url") ?: ""

          loadYoutubeVideo(ytUrl)
        }
      }
      .addOnFailureListener {

      }
  }

  private fun loadYoutubeVideo(youtubeUrl: String) {
    val embedUrl = convertToEmbedUrl(youtubeUrl)

    youtubeWebView.settings.javaScriptEnabled = true
    youtubeWebView.settings.pluginState = WebSettings.PluginState.ON
    youtubeWebView.webViewClient = WebViewClient()

    val html = """
      <html>
      <body style="margin:0;">
        <iframe width="100%" height="100%" src="$embedUrl" frameborder="0" allowfullscreen></iframe>
      </body>
      </html>
    """.trimIndent()

    youtubeWebView.loadData(html, "text/html", "utf-8")
  }

  private fun convertToEmbedUrl(youtubeUrl: String): String {
    return try {
      val uri = Uri.parse(youtubeUrl)
      val host = uri.host ?: ""


      //handle standar yt links
      if (host.contains("youtube.com")) {
        val videoId = uri.getQueryParameter("v")
        "https://www.youtube.com/embed/$videoId"
      }
      // handle short yt links
      else if (host.contains("youtu.be")) {
        val videoId = uri.lastPathSegment
        "https://www.youtube.com/embed/$videoId"
      } else {
        "" // unsupported format
      }
    } catch (e: Exception) {
    ""
    }
  }
}