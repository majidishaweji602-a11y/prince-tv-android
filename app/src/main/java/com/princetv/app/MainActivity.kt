
package com.princetv.app

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.json.JSONObject

@UnstableApi
class MainActivity : AppCompatActivity() {

    data class Channel(
        val name: String,
        val category: String,
        val streamUrl: String
    )

    private val dark = Color.rgb(8, 9, 14)
    private val panel = Color.rgb(24, 25, 34)
    private val gold = Color.rgb(224, 184, 78)
    private val white = Color.WHITE
    private val muted = Color.LTGRAY

    private val channels = mutableListOf<Channel>()

    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var playerView: PlayerView

    private var player: ExoPlayer? = null
    private var currentTab = "Mwanzo"
    private var autoRotateEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = dark
        window.navigationBarColor = dark

        loadChannels()

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }

        main.addView(createHeader())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }

        scroll.addView(content)

        main.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        main.addView(createBottomNavigation())
        setContentView(main)

        showHome()
    }

    private fun loadChannels() {
        try {
            val text = assets.open("channels.json")
                .bufferedReader()
                .use { it.readText() }

            val array = JSONObject(text).getJSONArray("channels")

            channels.clear()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                channels.add(
                    Channel(
                        name = item.optString("name", "Channel"),
                        category = item.optString("category", "TV"),
                        streamUrl = item.optString("streamUrl", "").trim()
                    )
                )
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Tatizo kusoma channels.json",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(18), dp(15))
            setBackgroundColor(dark)
        }

        val logo = TextView(this).apply {
            text = "P"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            background = shape(gold, 50)
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(46), dp(46))
        )

        val names = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, 0, 0)
        }

        names.addView(label("PRINCE TV", 21f, gold, true))
        names.addView(label("TANZANIA ENTERTAINMENT", 9f, muted))

        header.addView(
            names,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val live = TextView(this).apply {
            text = "● LIVE"
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(gold)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background = shape(panel, 30)
        }

        header.addView(live)
        return header
    }

    private fun showHome() {
        currentTab = "Mwanzo"
        content.removeAllViews()

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.rgb(75, 55, 18),
                    Color.rgb(29, 25, 20),
                    panel
                )
            ).apply {
                cornerRadius = dp(18).toFloat()
            }
        }

        hero.addView(label("KARIBU PRINCE TV", 12f, gold, true))
        hero.addView(label("Burudani yako, sehemu moja.", 21f, white, true))
        hero.addView(label("Tazama matangazo ya moja kwa moja.", 12f, muted))

        val watch = Button(this).apply {
            text = "▶  TAZAMA LIVE TV"
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.BLACK)
            setBackgroundColor(gold)
            setOnClickListener { showLive() }
        }

        hero.addView(
            watch,
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                topMargin = dp(15)
            }
        )

        content.addView(
            hero,
            LinearLayout.LayoutParams(-1, dp(205))
        )

        addSectionTitle("LIVE TV", "Tazama channel")
        addChannelCards(content)

        addSectionTitle("MOVIES", "Filamu")
        addMovieCards(content)

        addSectionTitle("SERIES", "Vipindi")
        addPlaceholder("Series zitakuja hapa")
    }

    private fun showLive() {
        currentTab = "Live TV"
        content.removeAllViews()

        addSectionTitle("LIVE TV", "Chagua channel uanze kutazama")

        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(Color.BLACK)
            setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)

            // Ficha muda uliochezwa na jumla ya muda.
            post {
                hidePlayerTimeLabels()
            }
        }

        content.addView(
            playerView,
            LinearLayout.LayoutParams(-1, dp(260)).apply {
                bottomMargin = dp(6)
            }
        )

        addPlayerSizeControls()

        status = label("Chagua channel kuanza kutazama", 12f, muted)
        content.addView(status)

        addSectionTitle("CHANNELS", "Channel zote")
        addChannelCards(content)

        val manual = EditText(this).apply {
            hint = "Bandika stream URL hapa"
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = shape(panel, 10)
        }

        content.addView(
            manual,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                topMargin = dp(14)
            }
        )

        val play = Button(this).apply {
            text = "CHEZA URL"
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.BLACK)
            setBackgroundColor(gold)
            setOnClickListener {
                playUrl(manual.text.toString().trim())
            }
        }

        content.addView(
            play,
            LinearLayout.LayoutParams(-1, dp(48))
        )
    }

    private fun hidePlayerTimeLabels() {
        val positionId = resources.getIdentifier(
            "exo_position", "id", packageName
        )

        val durationId = resources.getIdentifier(
            "exo_duration", "id", packageName
        )

        if (positionId != 0) {
            playerView.findViewById<View>(positionId)?.visibility = View.GONE
        }

        if (durationId != 0) {
            playerView.findViewById<View>(durationId)?.visibility = View.GONE
        }
    }

    private fun addPlayerSizeControls() {
        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(10))
        }

        fun addControl(title: String, action: () -> Unit) {
            val button = Button(this).apply {
                text = title
                isAllCaps = false
                textSize = 10f
                typeface = Typeface.create(
                    "sans-serif-medium",
                    Typeface.NORMAL
                )
                setTextColor(Color.BLACK)
                background = shape(gold, 9)
                setPadding(dp(8), 0, dp(8), 0)
                setOnClickListener { action() }
            }

            controls.addView(
                button,
                LinearLayout.LayoutParams(dp(92), dp(42)).apply {
                    marginEnd = dp(7)
                }
            )
        }

        addControl("PANUA") {
            playerView.setResizeMode(
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            )
        }

        addControl("JAZA") {
            playerView.setResizeMode(
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            )
        }

        addControl("RUDISHA") {
            playerView.setResizeMode(
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            )
            autoRotateEnabled = false
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        addControl("AUTO ROTATE") {
            autoRotateEnabled = !autoRotateEnabled

            requestedOrientation = if (autoRotateEnabled) {
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            Toast.makeText(
                this,
                if (autoRotateEnabled) {
                    "Auto rotate imewashwa"
                } else {
                    "Auto rotate imezimwa"
                },
                Toast.LENGTH_SHORT
            ).show()
        }

        horizontal.addView(controls)

        content.addView(
            horizontal,
            LinearLayout.LayoutParams(-1, dp(52))
        )
    }

    private fun showMovies() {
        currentTab = "Movies"
        content.removeAllViews()
        addSectionTitle("MOVIES", "Filamu")
        addMovieCards(content)
        addPlaceholder("Picha na filamu zitaongezwa hapa")
    }

    private fun showPackages() {
        currentTab = "Vifurushi"
        content.removeAllViews()
        addSectionTitle("VIFURUSHI", "Chagua kifurushi")
        addPlaceholder("Vifurushi vya PRINCE TV vitaonekana hapa")
    }

    private fun showAccount() {
        currentTab = "Akaunti"
        content.removeAllViews()
        addSectionTitle("AKAUNTI", "Karibu PRINCE TV")
        addPlaceholder("Taarifa za akaunti zitaonekana hapa")
    }

    private fun addChannelCards(parent: LinearLayout) {
        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        for (channel in channels) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(12), dp(10), dp(12))
                background = shape(panel, 14)
                setOnClickListener {
                    playChannel(channel)
                }
            }

            val icon = TextView(this).apply {
                text = "▶"
                textSize = 25f
                gravity = Gravity.CENTER
                setTextColor(gold)
                typeface = Typeface.create(
                    "sans-serif-medium",
                    Typeface.NORMAL
                )
                background = shape(Color.rgb(39, 37, 31), 50)
            }

            card.addView(
                icon,
                LinearLayout.LayoutParams(dp(55), dp(55))
            )

            card.addView(
                label(channel.name, 14f, white, true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(9), 0, dp(3))
                }
            )

            card.addView(
                label(channel.category.uppercase(), 10f, gold)
            )

            card.addView(
                label(
                    if (channel.streamUrl.isNotBlank()) "● LIVE" else "TV",
                    10f,
                    if (channel.streamUrl.isNotBlank()) gold else muted
                ).apply {
                    setPadding(0, dp(7), 0, 0)
                }
            )

            row.addView(
                card,
                LinearLayout.LayoutParams(dp(140), dp(155)).apply {
                    rightMargin = dp(10)
                }
            )
        }

        horizontal.addView(row)

        parent.addView(
            horizontal,
            LinearLayout.LayoutParams(-1, dp(165))
        )
    }

    private fun addMovieCards(parent: LinearLayout) {
        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val titles = listOf(
            "MOVIE 01",
            "MOVIE 02",
            "MOVIE 03",
            "MOVIE 04"
        )

        for (title in titles) {
            val poster = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = shape(panel, 12)
                setPadding(dp(8), dp(10), dp(8), dp(10))
            }

            poster.addView(label("🎬", 30f, gold))
            poster.addView(label(title, 11f, white, true))

            row.addView(
                poster,
                LinearLayout.LayoutParams(dp(115), dp(145)).apply {
                    rightMargin = dp(10)
                }
            )
        }

        horizontal.addView(row)
        parent.addView(horizontal)
    }

    private fun addSectionTitle(title: String, subtitle: String) {
        content.addView(
            label(title, 18f, gold, true).apply {
                setPadding(0, dp(18), 0, dp(3))
            }
        )

        content.addView(
            label(subtitle, 12f, muted).apply {
                setPadding(0, 0, 0, dp(10))
            }
        )
    }

    private fun addPlaceholder(message: String) {
        val box = TextView(this).apply {
            text = message
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(muted)
            background = shape(panel, 14)
            setPadding(dp(15), dp(25), dp(15), dp(25))
        }

        content.addView(
            box,
            LinearLayout.LayoutParams(-1, dp(95)).apply {
                topMargin = dp(8)
            }
        )
    }

    private fun createBottomNavigation(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(7), dp(3), dp(7))
            setBackgroundColor(Color.rgb(17, 18, 25))
        }

        val tabs = listOf(
            "⌂\nMwanzo",
            "▶\nLive TV",
            "▣\nMovies",
            "◆\nVifurushi",
            "●\nAkaunti"
        )

        for (tab in tabs) {
            val name = tab.substringAfter("\n")

            val item = TextView(this).apply {
                text = tab
                textSize = 10f
                gravity = Gravity.CENTER
                typeface = Typeface.create(
                    "sans-serif-medium",
                    Typeface.NORMAL
                )
                setTextColor(if (name == currentTab) gold else muted)
                setPadding(dp(2), dp(5), dp(2), dp(5))

                setOnClickListener {
                    when (name) {
                        "Mwanzo" -> showHome()
                        "Live TV" -> showLive()
                        "Movies" -> showMovies()
                        "Vifurushi" -> showPackages()
                        "Akaunti" -> showAccount()
                    }
                    refreshNavigation(nav)
                }
            }

            nav.addView(
                item,
                LinearLayout.LayoutParams(0, dp(52), 1f)
            )
        }

        return nav
    }

    private fun refreshNavigation(nav: LinearLayout) {
        for (i in 0 until nav.childCount) {
            val item = nav.getChildAt(i) as TextView
            val name = item.text.toString().substringAfter("\n")
            item.setTextColor(if (name == currentTab) gold else muted)
        }
    }

    private fun playChannel(channel: Channel) {
        if (channel.streamUrl.isBlank()) {
            Toast.makeText(
                this,
                "Stream ya ${channel.name} bado haijawekwa",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (currentTab != "Live TV") {
            showLive()
        }

        status.text = "Inaunganisha ${channel.name}..."
        playUrl(channel.streamUrl)
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
            Toast.makeText(this, "Weka URL sahihi", Toast.LENGTH_SHORT).show()
            return
        }

        if (!::playerView.isInitialized) {
            showLive()
        }

        releasePlayer()
        status.text = "Inaunganisha kwenye server..."

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("PRINCE TV Android")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("Accept" to "*/*"))

        val extractors = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                    DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
            )

        val mediaSourceFactory =
            DefaultMediaSourceFactory(this, extractors)
                .setDataSourceFactory(httpFactory)

        val itemBuilder = MediaItem.Builder().setUri(url)
        detectMimeType(url)?.let { itemBuilder.setMimeType(it) }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exo ->
                playerView.player = exo

                // Hakikisha muda hauonekani tena baada ya player kuanza.
                playerView.post {
                    hidePlayerTimeLabels()
                }

                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        status.text = when (state) {
                            Player.STATE_BUFFERING -> "Inapokea data..."
                            Player.STATE_READY -> "Video iko tayari kucheza"
                            Player.STATE_ENDED -> "Stream imeisha"
                            Player.STATE_IDLE -> "Player imesimama"
                            else -> "Inaandaa video..."
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        status.text =
                            "KOSA: ${error.errorCodeName}"
                    }
                })

                exo.setMediaItem(itemBuilder.build())
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun label(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)

            typeface = Typeface.create(
                if (bold) "sans-serif-bold" else "sans-serif",
                Typeface.NORMAL
            )

            includeFontPadding = false
            letterSpacing = 0.01f
        }
    }

    private fun shape(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
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
        player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
