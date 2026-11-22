
package com.princetv.app

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
import org.json.JSONObject

@UnstableApi
class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var urlInput: EditText
    private lateinit var channelList: LinearLayout

    private val gold = Color.rgb(214, 180, 90)
    private val dark = Color.rgb(9, 9, 16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = dark
        window.navigationBarColor = dark

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val brand = TextView(this).apply {
            text = "PRINCE TV"
            textSize = 26f
            setTextColor(gold)
            setPadding(0, dp(8), 0, dp(2))
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

        page.addView(brand)
        page.addView(tagline)

        page.addView(
            playerView,
            LinearLayout.LayoutParams(-1, dp(225)).apply {
                topMargin = dp(18)
                bottomMargin = dp(12)
            }
        )

        status = TextView(this).apply {
            text = "Chagua channel kuendelea"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(4), 0, dp(10))
        }
        page.addView(status)

        val heading = TextView(this).apply {
            text = "LIVE TV CHANNELS"
            textSize = 18f
            setTextColor(gold)
            setPadding(0, dp(12), 0, dp(12))
        }
        page.addView(heading)

        channelList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        page.addView(channelList)

        val manualHeading = TextView(this).apply {
            text = "PLAY STREAM URL"
            textSize = 16f
            setTextColor(gold)
            setPadding(0, dp(20), 0, dp(10))
        }
        page.addView(manualHeading)

        urlInput = EditText(this).apply {
            hint = "Bandika URL ya channel"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.rgb(31, 29, 42))
        }
        page.addView(urlInput, LinearLayout.LayoutParams(-1, dp(52)))

        val playButton = Button(this).apply {
            text = "CHEZA URL"
            setTextColor(Color.BLACK)
            setBackgroundColor(gold)
            setOnClickListener {
                playUrl(urlInput.text.toString().trim())
            }
        }

        page.addView(
            playButton,
            LinearLayout.LayoutParams(-1, dp(52))
        )

        val note = TextView(this).apply {
            text = "AUTO FORMAT: TS • HLS • DASH • MP4"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(16), 0, dp(20))
        }
        page.addView(note)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        }

        setContentView(scroll)

        loadChannels()
    }

    private fun loadChannels() {
        try {
            val jsonText = assets.open("channels.json")
                .bufferedReader()
                .use { it.readText() }

            val json = JSONObject(jsonText)
            val channels = json.getJSONArray("channels")

            channelList.removeAllViews()

            for (i in 0 until channels.length()) {

                val channel = channels.getJSONObject(i)

                val name = channel.getString("name")
                val category = channel.getString("category")
                val streamUrl = channel.optString(
                    "streamUrl", ""
                ).trim()

                val channelButton = Button(this).apply {
                    text = "$name   •   $category"
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setBackgroundColor(Color.rgb(31, 29, 42))

                    setOnClickListener {

                        if (streamUrl.isNotBlank()) {

                            status.text = "Inaunganisha $name..."

                            playUrl(streamUrl)

                        } else {

                            status.text =
                                "$name bado haina stream URL."

                            Toast.makeText(
                                this@MainActivity,
                                "Stream ya $name bado haijawekwa",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                channelList.addView(
                    channelButton,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(54)
                    ).apply {
                        bottomMargin = dp(7)
                    }
                )
            }

            status.text =
                "${channels.length()} channels zimepatikana"

        } catch (e: Exception) {

            status.text =
                "Imeshindwa kusoma channels.json: ${e.message}"
        }
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
        return (
            value * resources.displayMetrics.density
        ).toInt()
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
