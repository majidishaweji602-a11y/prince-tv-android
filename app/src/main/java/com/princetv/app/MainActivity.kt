
package com.princetv.app

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
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
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(225)
        ).apply {
            topMargin = dp(18)
            bottomMargin = dp(12)
        }

        status = TextView(this).apply {
            text = "Weka link ya channel"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 10)
        }

        urlInput = EditText(this).apply {
            hint = "Bandika URL ya channel"
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
            setOnClickListener {
                playUrl(urlInput.text.toString().trim())
            }
        }

        val note = TextView(this).apply {
            text = "AUTO FORMAT: TS • HLS • DASH • MP4"
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

    private fun detectMimeType(url: String): String? {
        val uri = Uri.parse(url)

        val extension = (
            uri.getQueryParameter("extension")
                ?: uri.lastPathSegment?.substringAfterLast('.', "")
                ?: ""
            ).lowercase()

        return when (extension) {
            "m3u8" -> MimeTypes.APPLICATION_M3U8
            "mpd" -> MimeTypes.APPLICATION_MPD
            "mp4", "m4v" -> MimeTypes.VIDEO_MP4
            "ts", "mpegts" -> MimeTypes.VIDEO_MP2T
            else -> null
        }
    }

    private fun playUrl(url: String) {

        if (
            url.isBlank() ||
            !(url.startsWith("https://", true) ||
              url.startsWith("http://", true))
        ) {
            Toast.makeText(
                this,
                "Weka URL sahihi",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        releasePlayer()
        status.text = "Inaunganisha kwenye server..."

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36"
            )
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf("Accept" to "*/*")
            )

        val extractors = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
            )

        val mediaSourceFactory =
            DefaultMediaSourceFactory(this, extractors)
                .setDataSourceFactory(httpFactory)

        val itemBuilder = MediaItem.Builder()
            .setUri(url)

        detectMimeType(url)?.let {
            itemBuilder.setMimeType(it)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exo ->

                playerView.player = exo

                exo.addListener(object : Player.Listener {

                    override fun onPlaybackStateChanged(
                        playbackState: Int
                    ) {
                        status.text = when (playbackState) {
                            Player.STATE_BUFFERING ->
                                "Inapokea data kutoka server..."

                            Player.STATE_READY ->
                                "Video iko tayari kucheza"

                            Player.STATE_ENDED ->
                                "Stream imeisha"

                            Player.STATE_IDLE ->
                                "Player imesimama"

                            else ->
                                "Inaandaa video..."
                        }
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {
                        status.text =
                            "KOSA: ${error.errorCodeName} — ${error.message ?: "Hakuna maelezo"}"
                    }
                })

                exo.setMediaItem(itemBuilder.build())
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun releasePlayer() {
        if (::playerView.isInitialized) {
            playerView.player = null
        }

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
