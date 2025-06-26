package com.example.quizguru

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainGameVideoActivity : AppCompatActivity() {

  private lateinit var quizVideo: WebView
  private lateinit var btnContinue: Button

  private lateinit var fStore: FirebaseFirestore
  private lateinit var videoIds: ArrayList<String>

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_game_video)

    fStore = FirebaseFirestore.getInstance()

    quizVideo = findViewById(R.id.wvVideoPlay)
    btnContinue = findViewById(R.id.btnContinue)

    videoIds = intent.getStringArrayListExtra("video_ids") ?: arrayListOf()
    val currentIndex = intent.getIntExtra("current_index", 0)
    val difficulty = intent.getStringExtra("difficulty") ?: ""
    val theme = intent.getStringExtra("theme") ?: ""

    btnContinue.isEnabled = false

    quizVideo.settings.javaScriptEnabled = true
    quizVideo.addJavascriptInterface(VideoPlayerInterface(), "AndroidInterface")
    quizVideo.settings.loadWithOverviewMode = true
    quizVideo.settings.useWideViewPort = true

    val videoId = videoIds.getOrNull(currentIndex) ?: ""
    fetchVideo(videoId)

    btnContinue.setOnClickListener {
      Log.d("DEBUG", "Sending video_ids=${videoIds}, current_index=$currentIndex, difficulty=$difficulty")

      val intent = Intent(this, MainGameDrillActivity::class.java)
      intent.putStringArrayListExtra("video_ids", videoIds)
      intent.putExtra("current_index", currentIndex)
      intent.putExtra("difficulty", difficulty)
      intent.putExtra("theme", theme)
      startActivity(intent)
      finish()
    }

  }

  private fun fetchVideo(videoId: String) {
    fStore.collection("videos")
      .document(videoId)
      .get()
      .addOnSuccessListener { video ->
        val youtubeUrl = video.getString("url") ?: ""
        val embedUrl = convertToEmbedUrl(youtubeUrl)
        val extractedVideoId = Uri.parse(embedUrl).lastPathSegment ?: ""

        val html = """
        <html>
          <body style="margin:0">
            <div id="player"></div>
            <script>
              var tag = document.createElement('script');
              tag.src = "https://www.youtube.com/iframe_api";
              var firstScriptTag = document.getElementsByTagName('script')[0];
              firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

              var player;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  height: '100%',
                  width: '100%',
                  videoId: '$extractedVideoId',
                  events: {
                    'onStateChange': onPlayerStateChange,
                    'onError': onPlayerError
                  }
                });
              }

              function onPlayerStateChange(event) {
                if (event.data == YT.PlayerState.ENDED) {
                  AndroidInterface.onVideoEnded();
                }
              }
              
              function onPlayerError(event) {
                console.log("YouTube Player Error:", event.data);
                AndroidInterface.onVideoEnded();
              }
            </script>
          </body>
        </html>
      """.trimIndent()

        quizVideo.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
      }
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

  inner class VideoPlayerInterface() {
    @JavascriptInterface
    fun onVideoEnded() {
      runOnUiThread {
        btnContinue.isEnabled = true
      }
    }

    @JavascriptInterface
    fun onVideoError() {
      runOnUiThread {
        btnContinue.isEnabled = true
        Toast.makeText(this@MainGameVideoActivity, "Video couldn't be played. You may continue.", Toast.LENGTH_SHORT).show()
      }
    }
  }
}