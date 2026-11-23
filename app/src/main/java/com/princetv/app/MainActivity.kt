package com.princetv.app

import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
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
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.json.JSONObject

@UnstableApi
class MainActivity : AppCompatActivity() {

    private data class Channel(
        val name: String,
        val category: String,
        val streamUrl: String,
        val icon: String
    )

    private data class HeroSlide(
        val eyebrow: String,
        val title: String,
        val description: String,
        val colors: IntArray
    )

    private val dark = Color.rgb(8, 9, 14)
    private val panel = Color.rgb(24, 25, 34)
    private val panelLight = Color.rgb(35, 35, 44)
    private val gold = Color.rgb(224, 184, 78)
    private val white = Color.WHITE
    private val muted = Color.rgb(178, 179, 188)

    private val channels = mutableListOf<Channel>()

    private lateinit var rootFrame: FrameLayout
    private lateinit var appShell: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var bottomNavigation: LinearLayout
    private lateinit var heroCard: LinearLayout
    private lateinit var heroEyebrow: TextView
    private lateinit var heroTitle: TextView
    private lateinit var heroDescription: TextView
    private lateinit var heroIndicators: LinearLayout
    private lateinit var status: TextView
    private lateinit var playerView: PlayerView

    private var player: ExoPlayer? = null
    private var playerContainer: FrameLayout? = null
    private var fullScreenButton: TextView? = null
    private var originalPlayerParent: ViewGroup? = null
    private var originalPlayerIndex = -1
    private var originalPlayerLayoutParams: ViewGroup.LayoutParams? = null
    private var isFullScreen = false
    private var autoRotateEnabled = false
    private var orientationBeforeFullScreen =
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var currentTab = "Mwanzo"
    private var heroSlideIndex = 0

    private val handler = Handler(Looper.getMainLooper())

    private val heroSlides = listOf(
        HeroSlide(
            "KARIBU PRINCE TV",
            "BURUDANI YAKO,\nSEHEMU MOJA",
            "Fungua PRINCE TV na uchague unachopenda kutazama.",
            intArrayOf(
                Color.rgb(91, 62, 18),
                Color.rgb(39, 31, 22),
                panel
            )
        ),
        HeroSlide(
            "LIVE ENTERTAINMENT",
            "Tazama LIVE TV",
            "Chagua channel kwenye orodha na uanze kutazama.",
            intArrayOf(
                Color.rgb(46, 54, 72),
                Color.rgb(29, 30, 42),
                panel
            )
        ),
        HeroSlide(
            "MOVIES & SERIES",
            "FILAMU NA VIPINDI",
            "Sehemu ya filamu na series imeandaliwa kwa ajili ya maudhui yajayo.",
            intArrayOf(
                Color.rgb(79, 42, 52),
                Color.rgb(39, 28, 37),
                panel
            )
        )
    )

    private val slideshowRunnable = object : Runnable {
        override fun run() {
            if (currentTab == "Mwanzo" && ::heroCard.isInitialized) {
                heroSlideIndex =
                    (heroSlideIndex + 1) % heroSlides.size
                updateHeroSlide()
                handler.postDelayed(this, 5000L)
            }
        }
    }

    private val hideFullScreenButtonRunnable = Runnable {
        if (isFullScreen) {
            fullScreenButton?.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = dark
        window.navigationBarColor = dark
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        loadChannels()

        rootFrame = FrameLayout(this).apply {
            setBackgroundColor(dark)
        }

        appShell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(dark)
        }

        appShell.addView(createHeader())

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(26))
        }

        scroll.addView(
            content,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        appShell.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        appShell.addView(createBottomNavigation())

        rootFrame.addView(
            appShell,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(rootFrame)
        showHome()
        showSplashScreen()
    }

    // PRINCE TV SPLASH SCREEN - 5 SECONDS
    private fun showSplashScreen() {
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(dark)
            elevation = dp(100).toFloat()
            isClickable = true
            isFocusable = true
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_prince_tv)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "PRINCE TV"
        }

        splash.addView(
            logo,
            LinearLayout.LayoutParams(dp(140), dp(140))
        )

        splash.addView(
            label("PRINCE TV", 29f, gold, true).apply {
                gravity = Gravity.CENTER
                letterSpacing = 0.08f
                setPadding(0, dp(14), 0, dp(5))
            }
        )

        splash.addView(
            label(
                "BURUDANI YAKO, SEHEMU MOJA",
                11f,
                muted,
                true
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        rootFrame.addView(
            splash,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                rootFrame.removeView(splash)
            }
        }, 5000L)
    }

    private fun loadChannels() {
        channels.clear()

        try {
            val jsonText = assets.open("channels.json")
                .bufferedReader()
                .use { it.readText() }

            val array = JSONObject(jsonText)
                .getJSONArray("channels")

            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                channels.add(
                    Channel(
                        name = item.optString("name", "Channel"),
                        category = item.optString("category", "TV"),
                        streamUrl = item.optString("streamUrl", "").trim(),
                        icon = item.optString("icon", "tv")
                    )
                )
            }
        } catch (error: Exception) {
            Toast.makeText(
                this,
                "Imeshindikana kusoma channels.json",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // HEADER ILIYOBORESHWA
    private fun createHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(14))
            setBackgroundColor(dark)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_prince_tv)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "PRINCE TV logo"
            translationY = dp(10).toFloat()
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(dp(72), dp(72))
        )

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(11), 0, 0, 0)
        }

        brand.addView(
            label("PRINCE TV", 31f, gold, true).apply {
                typeface = Typeface.create(
                    "sans-serif-black",
                    Typeface.NORMAL
                )
                letterSpacing = 0.10f
            }
        )

        brand.addView(
            label("TANZANIA ENTERTAINMENT", 9f, muted, true)
        )

        header.addView(
            brand,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val liveTag = TextView(this).apply {
            text = "●  LIVE"
            textSize = 10f
            typeface = Typeface.create(
                "sans-serif-bold",
                Typeface.NORMAL
            )
            setTextColor(gold)
            setPadding(dp(11), dp(8), dp(11), dp(8))
            background = shape(panel, 24)
        }

        header.addView(liveTag)
        return header
    }

    private fun showHome() {
        leavePlayerAndRelease()
        stopSlideshow()

        currentTab = "Mwanzo"
        content.removeAllViews()

        buildHero()

        addSectionTitle("LIVE TV", "Chagua channel yako")
        addChannelCards(content)

        addSectionTitle("MOVIES", "Filamu zitakazopatikana")
        addFeatureCards(
            content,
            listOf("ACTION", "DRAMA", "FAMILY", "COMEDY"),
            "MOVIES"
        )

        addSectionTitle("SERIES", "Vipindi na mfululizo")
        addFeatureCards(
            content,
            listOf(
                "LOCAL SERIES",
                "DRAMA SERIES",
                "DOCUMENTARY",
                "KIDS SERIES"
            ),
            "SERIES"
        )

        // Sports haziongezwi tena hapa kwa sababu zipo kwenye LIVE TV.
        refreshNavigation()
        startSlideshow()
    }

    private fun buildHero() {
        heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = heroBackground(
                heroSlides[heroSlideIndex].colors
            )
        }

        heroEyebrow = label("", 10f, gold, true)

        heroTitle = label("", 27f, white, true).apply {
            letterSpacing = 0.015f
            setPadding(0, dp(8), 0, dp(7))
        }

        heroDescription = label("", 12f, muted).apply {
            maxLines = 2
        }

        heroCard.addView(heroEyebrow)
        heroCard.addView(heroTitle)
        heroCard.addView(heroDescription)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }

        val watch = Button(this).apply {
            text = "▶  TAZAMA LIVE TV"
            isAllCaps = false
            textSize = 13f
            typeface = Typeface.create(
                "sans-serif-bold",
                Typeface.NORMAL
            )
            setTextColor(Color.BLACK)
            background = shape(gold, 9)
            setPadding(dp(14), 0, dp(14), 0)
            setOnClickListener {
                showLive()
            }
        }

        actions.addView(
            watch,
            LinearLayout.LayoutParams(-2, dp(48))
        )

        val explore = TextView(this).apply {
            text = "  Vinjari  ›"
            textSize = 12f
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
            setTextColor(white)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(9), 0, dp(8), 0)
            setOnClickListener {
                showLive()
            }
        }

        actions.addView(explore)
        heroCard.addView(actions)

        heroIndicators = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(13), 0, 0)
        }

        heroCard.addView(heroIndicators)

        content.addView(
            heroCard,
            LinearLayout.LayoutParams(-1, dp(245)).apply {
                bottomMargin = dp(7)
            }
        )

        updateHeroSlide()
    }

    private fun updateHeroSlide() {
        if (!::heroCard.isInitialized) return

        val slide = heroSlides[heroSlideIndex]

        heroCard.background = heroBackground(slide.colors)
        heroEyebrow.text = slide.eyebrow
        heroTitle.text = slide.title
        heroDescription.text = slide.description

        heroIndicators.removeAllViews()

        for (index in heroSlides.indices) {
            val dot = TextView(this).apply {
                text = if (index == heroSlideIndex) "●" else "○"
                textSize = if (index == heroSlideIndex) 11f else 9f
                setTextColor(
                    if (index == heroSlideIndex) gold else muted
                )
                setPadding(0, 0, dp(8), 0)

                setOnClickListener {
                    heroSlideIndex = index
                    updateHeroSlide()
                    startSlideshow()
                }
            }

            heroIndicators.addView(dot)
        }
    }

    private fun startSlideshow() {
        stopSlideshow()

        if (currentTab == "Mwanzo") {
            handler.postDelayed(slideshowRunnable, 5000L)
        }
    }

    private fun stopSlideshow() {
        handler.removeCallbacks(slideshowRunnable)
    }

    private fun showLive() {
        stopSlideshow()

        if (isFullScreen) {
            exitFullScreen()
        }

        if (::playerView.isInitialized) {
            releasePlayer()
        }

        currentTab = "Live TV"
        content.removeAllViews()

        addSectionTitle(
            "LIVE TV",
            "Chagua channel uanze kutazama"
        )

        playerView = PlayerView(this).apply {
            useController = true
            controllerShowTimeoutMs = 3500
            controllerHideOnTouch = true
            setBackgroundColor(Color.BLACK)
            setResizeMode(
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            )
            keepScreenOn = true
        }

        val playerFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        playerFrame.addView(
            playerView,
            FrameLayout.LayoutParams(-1, -1)
        )

        fullScreenButton = TextView(this).apply {
            text = "⛶"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(white)
            background = shape(
                Color.argb(190, 12, 12, 16),
                8
            )
            contentDescription = "Fungua full screen"
            elevation = dp(5).toFloat()
            setPadding(dp(5), 0, dp(5), 0)

            setOnClickListener {
                if (isFullScreen) {
                    exitFullScreen()
                } else {
                    enterFullScreen()
                }
            }
        }

        playerFrame.addView(
            fullScreenButton,
            FrameLayout.LayoutParams(
                dp(46),
                dp(42),
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = dp(8)
                marginEnd = dp(8)
            }
        )

        playerView.setOnTouchListener { _, event ->
            if (
                event.action == MotionEvent.ACTION_DOWN &&
                isFullScreen
            ) {
                revealFullScreenButton()
            }
            false
        }

        playerContainer = playerFrame

        content.addView(
            playerFrame,
            LinearLayout.LayoutParams(-1, dp(238)).apply {
                bottomMargin = dp(5)
            }
        )

        addPlayerSizeControls()

        status = label(
            "Chagua channel kuanza kutazama",
            12f,
            muted
        ).apply {
            setPadding(0, dp(3), 0, dp(8))
        }

        content.addView(status)

        addSectionTitle(
            "CHANNELS",
            "Channel zote zilizopo"
        )

        addChannelCards(content)

        val manual = EditText(this).apply {
            hint = "Bandika stream URL hapa"
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 14f
            setSingleLine(true)

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_URI

            setPadding(dp(13), dp(8), dp(13), dp(8))
            background = shape(panel, 10)
        }

        content.addView(
            manual,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                topMargin = dp(12)
                bottomMargin = dp(8)
            }
        )

        val playButton = Button(this).apply {
            text = "CHEZA URL"
            isAllCaps = false
            textSize = 12f
            typeface = Typeface.create(
                "sans-serif-bold",
                Typeface.NORMAL
            )
            setTextColor(Color.BLACK)
            background = shape(gold, 9)

            setOnClickListener {
                playUrl(manual.text.toString().trim())
            }
        }

        content.addView(
            playButton,
            LinearLayout.LayoutParams(-1, dp(46))
        )

        refreshNavigation()
    }

    private fun addPlayerSizeControls() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(8))
        }

        fun addIcon(
            symbol: String,
            description: String,
            action: () -> Unit
        ) {
            val icon = TextView(this).apply {
                text = symbol
                textSize = 20f
                gravity = Gravity.CENTER

                setTextColor(
                    if (
                        description == "Auto rotate" &&
                        autoRotateEnabled
                    ) {
                        gold
                    } else {
                        white
                    }
                )

                contentDescription = description
                background = shape(panelLight, 24)
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    action()
                }
            }

            row.addView(
                icon,
                LinearLayout.LayoutParams(
                    dp(43),
                    dp(40)
                ).apply {
                    marginEnd = dp(9)
                }
            )
        }

        addIcon("⤢", "Zoom") {
            playerView.resizeMode =
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }

        addIcon("▣", "Fill") {
            playerView.resizeMode =
                AspectRatioFrameLayout.RESIZE_MODE_FILL
        }

        addIcon("⊡", "Fit") {
            playerView.resizeMode =
                AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        addIcon("⟳", "Auto rotate") {
            autoRotateEnabled = !autoRotateEnabled

            requestedOrientation = if (autoRotateEnabled) {
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            } else if (isFullScreen) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

            val button =
                row.getChildAt(row.childCount - 1) as? TextView

            button?.setTextColor(
                if (autoRotateEnabled) gold else white
            )

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

        content.addView(
            row,
            LinearLayout.LayoutParams(-1, dp(49))
        )
    }

    private fun enterFullScreen() {
        val container = playerContainer ?: return
        val parent = container.parent as? ViewGroup ?: return

        originalPlayerParent = parent
        originalPlayerIndex = parent.indexOfChild(container)
        originalPlayerLayoutParams = container.layoutParams

        orientationBeforeFullScreen = requestedOrientation

        parent.removeView(container)

        appShell.visibility = View.GONE

        rootFrame.addView(
            container,
            FrameLayout.LayoutParams(
                -1,
                -1,
                Gravity.CENTER
            )
        )

        isFullScreen = true

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )

        revealFullScreenButton()
    }

    private fun exitFullScreen() {
        if (!isFullScreen) return

        val container = playerContainer

        if (container != null) {
            (container.parent as? ViewGroup)
                ?.removeView(container)

            val parent = originalPlayerParent
            val params = originalPlayerLayoutParams

            if (parent != null && params != null) {
                val index = originalPlayerIndex.coerceIn(
                    0,
                    parent.childCount
                )

                parent.addView(
                    container,
                    index,
                    params
                )
            }
        }

        appShell.visibility = View.VISIBLE
        isFullScreen = false

        requestedOrientation = orientationBeforeFullScreen

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_VISIBLE

        handler.removeCallbacks(
            hideFullScreenButtonRunnable
        )

        fullScreenButton?.visibility = View.VISIBLE

        originalPlayerParent = null
        originalPlayerLayoutParams = null
        originalPlayerIndex = -1
    }

    private fun revealFullScreenButton() {
        fullScreenButton?.visibility = View.VISIBLE

        handler.removeCallbacks(
            hideFullScreenButtonRunnable
        )

        handler.postDelayed(
            hideFullScreenButtonRunnable,
            3500L
        )
    }

    private fun showMovies() {
        leavePlayerAndRelease()
        stopSlideshow()

        currentTab = "Movies"
        content.removeAllViews()

        addSectionTitle(
            "MOVIES",
            "Filamu na burudani"
        )

        addFeatureCards(
            content,
            listOf(
                "ACTION",
                "DRAMA",
                "FAMILY",
                "COMEDY",
                "ADVENTURE",
                "ROMANCE"
            ),
            "MOVIES"
        )

        addInfoPanel(
            "SEHEMU YA MOVIES",
            "Orodha ya filamu itaongezwa hapa baada ya kuwekwa maudhui na ruhusa zake."
        )

        refreshNavigation()
    }

    private fun showSeries() {
        leavePlayerAndRelease()
        stopSlideshow()

        currentTab = "Series"
        content.removeAllViews()

        addSectionTitle(
            "SERIES",
            "Vipindi na mfululizo"
        )

        addFeatureCards(
            content,
            listOf(
                "LOCAL SERIES",
                "DRAMA SERIES",
                "DOCUMENTARY",
                "KIDS SERIES",
                "COMEDY",
                "ACTION"
            ),
            "SERIES"
        )

        addInfoPanel(
            "SEHEMU YA SERIES",
            "Vipindi vitaonekana hapa vitakapoongezwa kwenye maktaba ya PRINCE TV."
        )

        refreshNavigation()
    }

    private fun showPackages() {
        leavePlayerAndRelease()
        stopSlideshow()

        currentTab = "Vifurushi"
        content.removeAllViews()

        addSectionTitle(
            "VIFURUSHI",
            "Chagua kifurushi"
        )

        addInfoPanel(
            "VIFURUSHI VYA PRINCE TV",
            "Taarifa za vifurushi zitaonyeshwa hapa zitakapowekwa."
        )

        refreshNavigation()
    }

    private fun showAccount() {
        leavePlayerAndRelease()
        stopSlideshow()

        currentTab = "Akaunti"
        content.removeAllViews()

        addSectionTitle(
            "AKAUNTI",
            "Karibu PRINCE TV"
        )

        addInfoPanel(
            "AKAUNTI YAKO",
            "Sehemu ya taarifa za akaunti itaongezwa hapa."
        )

        refreshNavigation()
    }

    private fun addChannelCards(parent: LinearLayout) {
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        for (channel in channels) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(
                    dp(9),
                    dp(11),
                    dp(9),
                    dp(10)
                )
                background = shape(panel, 13)
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    playChannel(channel)
                }
            }

            val logo = TextView(this).apply {
                text = channelInitials(channel.name)
                textSize = 25f
                gravity = Gravity.CENTER

                typeface = Typeface.create(
                    "sans-serif-black",
                    Typeface.NORMAL
                )

                setTextColor(
                    if (channel.icon == "news") white else gold
                )

                background = logoBackground(channel.icon)
            }

            card.addView(
                logo,
                LinearLayout.LayoutParams(
                    dp(88),
                    dp(88)
                )
            )

            card.addView(
                label(channel.name, 13f, white, true).apply {
                    gravity = Gravity.CENTER
                    maxLines = 2
                    setPadding(0, dp(8), 0, dp(3))
                }
            )

            card.addView(
                label(
                    channel.category.uppercase(),
                    9f,
                    gold,
                    true
                )
            )

            card.addView(
                label(
                    if (channel.streamUrl.isNotBlank()) {
                        "●  LIVE"
                    } else {
                        "HAIPO"
                    },
                    9f,
                    if (channel.streamUrl.isNotBlank()) {
                        gold
                    } else {
                        muted
                    },
                    true
                ).apply {
                    setPadding(0, dp(7), 0, 0)
                }
            )

            row.addView(
                card,
                LinearLayout.LayoutParams(
                    dp(158),
                    dp(184)
                ).apply {
                    marginEnd = dp(9)
                }
            )
        }

        scroll.addView(row)

        parent.addView(
            scroll,
            LinearLayout.LayoutParams(-1, dp(190))
        )
    }

    private fun addCategoryChannelCards(
        parent: LinearLayout,
        category: String
    ) {
        val matching = channels.filter {
            it.category.equals(
                category,
                ignoreCase = true
            )
        }

        if (matching.isEmpty()) {
            addInfoPanel(
                "HAKUNA CHANNEL",
                "Hakuna channel za kundi hili kwa sasa."
            )
            return
        }

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        for (channel in matching) {
            val chip = TextView(this).apply {
                text =
                    "${channelInitials(channel.name)}  ${channel.name}"

                textSize = 12f

                typeface = Typeface.create(
                    "sans-serif-bold",
                    Typeface.NORMAL
                )

                setTextColor(white)
                gravity = Gravity.CENTER
                background = shape(panel, 10)

                setPadding(
                    dp(13),
                    dp(12),
                    dp(13),
                    dp(12)
                )

                setOnClickListener {
                    playChannel(channel)
                }
            }

            row.addView(
                chip,
                LinearLayout.LayoutParams(
                    -2,
                    dp(46)
                ).apply {
                    marginEnd = dp(8)
                }
            )
        }

        scroll.addView(row)

        parent.addView(
            scroll,
            LinearLayout.LayoutParams(-1, dp(53))
        )
    }

    private fun addFeatureCards(
        parent: LinearLayout,
        titles: List<String>,
        type: String
    ) {
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        for ((index, title) in titles.withIndex()) {
            val artwork = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.START

                setPadding(
                    dp(12),
                    dp(12),
                    dp(10),
                    dp(11)
                )

                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    featureColors(index)
                ).apply {
                    cornerRadius = dp(13).toFloat()
                }
            }

            artwork.addView(
                label(
                    if (type == "MOVIES") "FILAMU" else "VIPINDI",
                    10f,
                    gold,
                    true
                )
            )

            artwork.addView(
                label(title, 15f, white, true).apply {
                    setPadding(0, dp(5), 0, dp(3))
                    maxLines = 2
                }
            )

            artwork.addView(
                label("ITAONGEZWA", 9f, muted, true)
            )

            row.addView(
                artwork,
                LinearLayout.LayoutParams(
                    dp(160),
                    dp(188)
                ).apply {
                    marginEnd = dp(9)
                }
            )
        }

        scroll.addView(row)

        parent.addView(
            scroll,
            LinearLayout.LayoutParams(-1, dp(196))
        )
    }

    private fun addInfoPanel(
        title: String,
        message: String
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(15),
                dp(17),
                dp(15),
                dp(17)
            )
            background = shape(panel, 13)
        }

        box.addView(
            label(title, 13f, gold, true)
        )

        box.addView(
            label(message, 12f, muted).apply {
                setPadding(0, dp(7), 0, 0)
            }
        )

        content.addView(
            box,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(6)
                bottomMargin = dp(10)
            }
        )
    }

    private fun addSectionTitle(
        title: String,
        subtitle: String
    ) {
        content.addView(
            label(title, 18f, gold, true).apply {
                letterSpacing = 0.025f
                setPadding(
                    0,
                    dp(17),
                    0,
                    dp(4)
                )
            }
        )

        content.addView(
            label(subtitle, 11f, muted).apply {
                setPadding(0, 0, 0, dp(10))
            }
        )
    }

    private fun styleTab(item: TextView, name: String) {
        val selected = name == currentTab
        item.setTextColor(if (selected) gold else muted)
        item.background = if (selected) {
            GradientDrawable().apply {
                cornerRadius = dp(30).toFloat()
                setColor(Color.rgb(22, 26, 34))
            }
        } else null
    }

    private fun createBottomNavigation(): View {
        bottomNavigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(10))
            setBackgroundColor(Color.rgb(10, 11, 14))
            elevation = dp(8).toFloat()
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
                text = SpannableString(tab).apply {
                    setSpan(
                        RelativeSizeSpan(2.4f), 0, 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                textSize = 14f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(dp(4), dp(8), dp(4), dp(8))
                setOnClickListener {
                    when (name) {
                        "Mwanzo" -> showHome()
                        "Live TV" -> showLive()
                        "Movies" -> showMovies()
                        "Vifurushi" -> showPackages()
                        "Akaunti" -> showAccount()
                    }
                }
            }
            styleTab(item, name)

            bottomNavigation.addView(
                item,
                LinearLayout.LayoutParams(0, dp(84), 1f)
                    .apply { setMargins(dp(2), 0, dp(2), 0) }
            )
        }

        return bottomNavigation
    }

    private fun refreshNavigation() {
        if (!::bottomNavigation.isInitialized) return

        for (index in 0 until bottomNavigation.childCount) {
            val item =
                bottomNavigation.getChildAt(index) as? TextView
                    ?: continue
            styleTab(item, item.text.toString().substringAfter("\n"))
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
            !(
                url.startsWith("https://", true) ||
                    url.startsWith("http://", true)
                )
        ) {
            Toast.makeText(
                this,
                "Weka URL sahihi inayoanza na http:// au https://",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (
            !::playerView.isInitialized ||
            currentTab != "Live TV"
        ) {
            showLive()
        }

        releasePlayer()
        status.text = "Inaunganisha kwenye server..."

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 PRINCE-TV"
            )
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )

        val extractors = DefaultExtractorsFactory()
            .setTsExtractorFlags(
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS or
                    DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES
            )

        val mediaSourceFactory =
            DefaultMediaSourceFactory(this, extractors)
                .setDataSourceFactory(httpFactory)
                .setLoadErrorHandlingPolicy(
                    DefaultLoadErrorHandlingPolicy(3)
                )

        val itemBuilder = MediaItem.Builder().setUri(url)

        detectMimeType(url)?.let {
            itemBuilder.setMimeType(it)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { exo ->

                playerView.player = exo

                exo.addListener(
                    object : Player.Listener {

                        override fun onPlaybackStateChanged(
                            playbackState: Int
                        ) {
                            status.text = when (playbackState) {
                                Player.STATE_BUFFERING ->
                                    "Inapokea data..."

                                Player.STATE_READY ->
                                    "Inacheza"

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
                                "Imeshindwa kucheza: ${error.errorCodeName}"

                            Toast.makeText(
                                this@MainActivity,
                                "Tatizo la stream: ${error.errorCodeName}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )

                exo.setMediaItem(itemBuilder.build())
                exo.prepare()
                exo.playWhenReady = true
            }
    }

    private fun leavePlayerAndRelease() {
        if (isFullScreen) {
            exitFullScreen()
        }

        if (::playerView.isInitialized) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        if (::playerView.isInitialized) {
            playerView.player = null
            playerView.keepScreenOn = false
        }

        player?.release()
        player = null
    }

    private fun channelInitials(name: String): String {
        val words = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return when {
            words.size >= 2 ->
                "${words[0].first()}${words[1].first()}".uppercase()

            words.isNotEmpty() ->
                words[0].take(3).uppercase()

            else ->
                "TV"
        }
    }

    private fun logoBackground(icon: String): GradientDrawable {
        val colors = when (icon.lowercase()) {
            "news" -> intArrayOf(
                Color.rgb(35, 62, 86),
                Color.rgb(18, 34, 51)
            )

            "sports" -> intArrayOf(
                Color.rgb(72, 57, 23),
                Color.rgb(42, 35, 18)
            )

            "kids" -> intArrayOf(
                Color.rgb(65, 43, 79),
                Color.rgb(37, 28, 49)
            )

            "movie" -> intArrayOf(
                Color.rgb(83, 42, 46),
                Color.rgb(45, 27, 33)
            )

            else -> intArrayOf(
                Color.rgb(74, 61, 34),
                Color.rgb(39, 34, 24)
            )
        }

        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors
        ).apply {
            cornerRadius = dp(12).toFloat()
            setStroke(
                dp(1),
                Color.argb(75, 224, 184, 78)
            )
        }
    }

    private fun featureColors(index: Int): IntArray {
        return when (index % 4) {
            0 -> intArrayOf(
                Color.rgb(91, 57, 35),
                Color.rgb(37, 29, 27)
            )

            1 -> intArrayOf(
                Color.rgb(53, 58, 83),
                Color.rgb(27, 29, 43)
            )

            2 -> intArrayOf(
                Color.rgb(73, 47, 67),
                Color.rgb(35, 27, 39)
            )

            else -> intArrayOf(
                Color.rgb(63, 65, 40),
                Color.rgb(31, 34, 25)
            )
        }
    }

    private fun heroBackground(
        colors: IntArray
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            colors
        ).apply {
            cornerRadius = dp(18).toFloat()
            setStroke(
                dp(1),
                Color.argb(45, 224, 184, 78)
            )
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
                if (bold) {
                    "sans-serif-bold"
                } else {
                    "sans-serif"
                },
                Typeface.NORMAL
            )

            includeFontPadding = false
            letterSpacing = 0.01f
        }
    }

    private fun shape(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()

        if (currentTab == "Mwanzo") {
            startSlideshow()
        }
    }

    override fun onPause() {
        stopSlideshow()
        super.onPause()
    }

    override fun onStop() {
        player?.pause()
        super.onStop()
    }

    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {
        super.onConfigurationChanged(newConfig)

        rootFrame.requestLayout()
        playerContainer?.requestLayout()
    }

    @Deprecated(
        "Deprecated in Android API; retained for Back compatibility"
    )
    override fun onBackPressed() {
        if (isFullScreen) {
            exitFullScreen()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        stopSlideshow()

        handler.removeCallbacks(
            hideFullScreenButtonRunnable
        )

        if (isFullScreen) {
            exitFullScreen()
        }

        releasePlayer()
        super.onDestroy()
    }
}
