package com.princetv.app

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.PlayerView

@UnstableApi
class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var urlInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(9, 9, 16)
        window.navigationBarColor = Color.rgb(9, 9, 16)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 9, 16))
            setPadding(18, 18, 18, 18)
        }

        val brand = TextView(this).apply {
            text = "PRINCE TV"
            textSize = 25f
            setTextColor(Color.rgb(214, 180, 90))
            setPadding(0, 8, 0, 2)
        }
        val tagline = TextView(this).apply {
            text = "TANZANIA LIVE TV"
            textSize = 11f
            setTextColor(Color.LTGRAY)
        }
        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(Color.BLACK)
        }
        val playerParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(225)
        ).apply { topMargin = dp(18); bottomMargin = dp(12) }

        status = TextView(this).apply {
            text = "Weka link ya channel unayotaka kujaribu"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 10)
        }
        urlInput = EditText(this).apply {
            hint = "Bandika stream URL (.ts / .m3u8 / .mpd)"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(31, 29, 42))
        }
        val playButton = Button(this).apply {
            text = "CHEZA CHANNEL"
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.rgb(214, 180, 90))
            setOnClickListener { playUrl(urlInput.text.toString().trim()) }
        }
        val note = TextView(this).apply {
            text = "MPEG-TS, HLS na DASH • Native Android Player"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(16), 0, 0)
        }

        page.addView(brand)
        page.addView(tagline)
        page.addView(playerView, playerParams)
        page.addView(status)
        page.addView(urlInput, LinearLayout.LayoutParams(-1, dp(52)))
        page.addView(playButton, LinearLayout.LayoutParams(-1, dp(52)))
        page.addView(note)
        setContentView(page)
    }

    private fun playUrl(url: String) {
        if (url.isBlank() || !(url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(this, "Weka URL sahihi ya channel", Toast.LENGTH_SHORT).show()
            return
        }
        releasePlayer()
        val extractors = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                TsExtractor.FLAG_DETECT_ACCESS_UNITS or
                    TsExtractor.FLAG_ALLOW_NON_IDR_KEYFRAMES
            )
        val mediaSourceFactory = DefaultMediaSourceFactory(this, extractors)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().also { exo ->
                playerView.player = exo
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        status.text = when (playbackState) {
                            Player.STATE_BUFFERING -> "Inapakia channel..."
                            Player.STATE_READY -> "Channel iko tayari kucheza"
                            Player.STATE_ENDED -> "Stream imeisha"
                            else -> "Inaandaa player..."
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        status.text = "Imeshindwa kucheza: ${error.errorCodeName}"
                    }
                })
                exo.setMediaItem(MediaItem.fromUri(url))
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun releasePlayer() {
        playerView.player = null
        player?.release()
        player = null
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
