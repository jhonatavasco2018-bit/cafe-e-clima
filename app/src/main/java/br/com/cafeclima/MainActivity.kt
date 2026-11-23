package br.com.cafeclima

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.os.Looper
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.*
import android.os.Handler
import android.os.Looper as MainLooper
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {
    // Tema Café & Clima: verde floresta, carvão e detalhes em lima/dourado
    private val green = Color.rgb(177, 213, 83)
    private val dark = Color.rgb(244, 246, 237)
    private val muted = Color.rgb(174, 187, 170)
    private val pageBackground = Color.rgb(13, 24, 18)
    private val panel = Color.rgb(25, 40, 31)
    private val panelBorder = Color.rgb(54, 76, 57)
    private val gold = Color.rgb(220, 174, 75)
    private lateinit var content: LinearLayout
    private lateinit var mainScroll: ScrollView
    private lateinit var quoteSection: View
    private lateinit var dollarSection: View
    private lateinit var weatherSection: View
    private lateinit var newsSection: View
    private lateinit var quoteStatus: TextView
    private lateinit var weatherStatus: TextView
    private lateinit var currentWeather: TextView
    private lateinit var forecastBox: LinearLayout
    private lateinit var quoteArabica: TextView
    private lateinit var quoteArabicaRio: TextView
    private lateinit var quoteConilon: TextView
    private lateinit var quoteUpdated: TextView
    private lateinit var dollarValue: TextView
    private lateinit var dollarStatus: TextView
    private lateinit var newsBox: LinearLayout
    private lateinit var newsStatus: TextView
    private var locationManager: LocationManager? = null
    private val locationPermissionRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        refreshQuotes()
        refreshDollar()
        refreshCoffeeNews()
        requestWeatherLocation()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun text(value: String, size: Float, color: Int = dark, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun roundedBackground(color: Int, stroke: Int = panelBorder, radius: Int = 18): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }
    }

    private fun styleButton(button: Button) {
        button.setTextColor(Color.rgb(18, 29, 20))
        button.textSize = 14f
        button.typeface = Typeface.DEFAULT_BOLD
        button.background = roundedBackground(green, green, 14)
        button.backgroundTintList = ColorStateList.valueOf(green)
        button.setPadding(dp(14), dp(10), dp(14), dp(10))
        button.elevation = dp(3).toFloat()
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBackground(panel)
            elevation = dp(3).toFloat()
        }
    }

    private fun addCard(parent: LinearLayout, child: View, top: Int = 12) {
        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(top)
        parent.addView(child, params)
    }


    private fun navItem(label: String, target: View): TextView {
        return text(label, 11f, green, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(4), dp(3), dp(4))
            isClickable = true
            background = roundedBackground(panel, panelBorder, 12)
            setOnClickListener {
                mainScroll.post { mainScroll.smoothScrollTo(0, target.top) }
            }
        }
    }


    private fun createSplashOverlay(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(18, 34, 24))
            isClickable = true
        }
        val landscape = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()
                paint.shader = LinearGradient(0f, 0f, 0f, h,
                    Color.rgb(35, 58, 42), Color.rgb(13, 25, 18), Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w, h, paint)
                paint.shader = null
                paint.color = Color.rgb(238, 178, 91)
                canvas.drawCircle(w * .78f, h * .29f, w * .105f, paint)
                val hill = Path().apply {
                    moveTo(0f, h * .56f); quadTo(w * .35f, h * .38f, w * .62f, h * .57f)
                    quadTo(w * .83f, h * .43f, w, h * .55f); lineTo(w, h); lineTo(0f, h); close()
                }
                paint.color = Color.rgb(42, 76, 48); canvas.drawPath(hill, paint)
                val hill2 = Path().apply {
                    moveTo(0f, h * .69f); quadTo(w * .45f, h * .50f, w, h * .68f)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                paint.color = Color.rgb(24, 53, 34); canvas.drawPath(hill2, paint)
                for (row in 0..7) {
                    val y = h * (.72f + row * .045f)
                    for (col in -1..7) {
                        val x = col * w / 6f + (row % 2) * w / 12f
                        val radius = w * (.025f + row * .003f)
                        paint.color = if ((row + col) % 2 == 0) Color.rgb(48, 91, 48) else Color.rgb(58, 103, 52)
                        canvas.drawCircle(x, y, radius, paint)
                        paint.color = Color.rgb(22, 48, 29)
                        canvas.drawRect(x - radius * .22f, y, x + radius * .22f, h, paint)
                    }
                }
            }
        }
        overlay.addView(landscape, FrameLayout.LayoutParams(-1, -1))
        val shade = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.argb(35, 5, 15, 8), Color.argb(150, 5, 15, 8)))
        }
        overlay.addView(shade, FrameLayout.LayoutParams(-1, -1))
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), 0, dp(28), 0)
        }
        brand.addView(text("☕", 58f, gold, true).apply { gravity = Gravity.CENTER })
        brand.addView(text("CAFÉ & CLIMA", 30f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = .08f
            setPadding(0, dp(10), 0, dp(8))
        })
        brand.addView(text("O campo, o mercado e o tempo em um só lugar", 14f, Color.rgb(232, 224, 196)).apply {
            gravity = Gravity.CENTER
        })
        brand.addView(text("MERCADO  •  CLIMA  •  DÓLAR", 11f, gold, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, 0)
            letterSpacing = .12f
        })
        overlay.addView(brand, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        return overlay
    }

    private fun buildScreen() {
        window.statusBarColor = pageBackground
        window.navigationBarColor = pageBackground
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply {
            setBackgroundColor(pageBackground)
            isFillViewport = true
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(26))
        }
        scroll.addView(content)
        mainScroll = scroll
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBackground)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // Cabeçalho do painel
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(18))
        }
        header.addView(text("☕", 34f, dark).apply { gravity = Gravity.CENTER })
        header.addView(text("CAFÉ & CLIMA", 25f, dark, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.06f
            setPadding(0, dp(2), 0, dp(3))
        })
        header.addView(text("MERCADO • CLIMA • DÓLAR • NOTÍCIAS", 10f, green, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        })
        content.addView(header)

        // Mercado: cotações em um cartão de destaque
        content.addView(text("MERCADO DO CAFÉ", 13f, muted, true).apply {
            letterSpacing = 0.08f
            setPadding(dp(3), dp(4), 0, dp(2))
        })
        val quoteCard = card()
        quoteSection = quoteCard
        quoteCard.setPadding(dp(16), dp(16), dp(16), dp(16))
        quoteCard.addView(text("📈  COTAÇÕES DO CAFÉ", 17f, green, true))
        quoteCard.addView(text("Referências nacionais • saca de 60 kg", 12f, muted).apply {
            setPadding(0, dp(4), 0, dp(12))
        })
        quoteArabica = text("Arábica • bebida dura: consultando…", 15f, dark, true)
        quoteArabicaRio = text("Arábica • bebida rio: consultando…", 15f, dark, true)
        quoteConilon = text("Conilon • tipo 7/8: consultando…", 15f, dark, true)
        quoteCard.addView(quoteArabica)
        quoteCard.addView(quoteArabicaRio.apply { setPadding(0, dp(9), 0, 0) })
        quoteCard.addView(quoteConilon.apply { setPadding(0, dp(9), 0, 0) })
        quoteUpdated = text("CCCV • preços de referência", 11f, muted)
        quoteUpdated.setPadding(0, dp(10), 0, 0)
        quoteCard.addView(quoteUpdated)
        quoteStatus = text("Buscando preços atualizados…", 11f, muted)
        quoteStatus.setPadding(0, dp(5), 0, dp(10))
        quoteCard.addView(quoteStatus)
        val quoteButton = Button(this).apply {
            text = "CONSULTAR CCCV  ↗"
            styleButton(this)
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cccv.org.br/cotacao/")))
            }
        }
        quoteCard.addView(quoteButton)
        addCard(content, quoteCard, 6)

        // Dólar em cartão mais compacto
        val dollarCard = card()
        dollarSection = dollarCard
        dollarCard.setPadding(dp(16), dp(14), dp(16), dp(14))
        val dollarHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        dollarHeader.addView(text("💵  DÓLAR COMERCIAL", 16f, green, true), LinearLayout.LayoutParams(0, -2, 1f))
        dollarHeader.addView(text("USD / BRL", 10f, muted, true))
        dollarCard.addView(dollarHeader)
        dollarValue = text("Consultando dólar…", 27f, dark, true)
        dollarValue.setPadding(0, dp(8), 0, dp(1))
        dollarCard.addView(dollarValue)
        dollarStatus = text("Buscando cotação atualizada…", 11f, muted)
        dollarStatus.setPadding(0, dp(2), 0, 0)
        dollarCard.addView(dollarStatus)
        addCard(content, dollarCard, 10)

        // Clima ganha destaque visual e vem antes das notícias
        content.addView(text("TEMPO NA SUA REGIÃO", 13f, muted, true).apply {
            letterSpacing = 0.08f
            setPadding(dp(3), dp(18), 0, dp(2))
        })
        val weatherCard = card()
        weatherSection = weatherCard
        weatherCard.setPadding(dp(16), dp(16), dp(16), dp(16))
        weatherCard.addView(text("🌦️  CLIMA NA SUA LOCALIZAÇÃO", 17f, green, true))
        weatherStatus = text("Obtendo localização do celular…", 11f, muted)
        weatherStatus.setPadding(0, dp(5), 0, dp(10))
        weatherCard.addView(weatherStatus)
        currentWeather = text("Aguardando previsão…", 16f, dark)
        currentWeather.setLineSpacing(dp(3).toFloat(), 1f)
        currentWeather.setPadding(0, 0, 0, dp(12))
        weatherCard.addView(currentWeather)
        weatherCard.addView(text("PRÓXIMOS 7 DIAS", 13f, green, true).apply {
            setPadding(0, dp(4), 0, dp(8))
        })
        forecastBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        weatherCard.addView(forecastBox)
        addCard(content, weatherCard, 6)

        // Notícias em cartão separado, com manchetes clicáveis
        content.addView(text("FIQUE POR DENTRO", 13f, muted, true).apply {
            letterSpacing = 0.08f
            setPadding(dp(3), dp(18), 0, dp(2))
        })
        val newsCard = card()
        newsSection = newsCard
        newsCard.setPadding(dp(16), dp(16), dp(16), dp(16))
        newsCard.addView(text("📰  NOTÍCIAS DO CAFÉ", 17f, green, true))
        newsCard.addView(text("Arábica, conilon e mercado", 12f, muted).apply {
            setPadding(0, dp(4), 0, dp(8))
        })
        newsStatus = text("Carregando notícias…", 11f, muted)
        newsCard.addView(newsStatus)
        newsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        newsCard.addView(newsBox)
        addCard(content, newsCard, 6)

        val refresh = Button(this).apply {
            text = "↻  ATUALIZAR INFORMAÇÕES"
            styleButton(this)
            setOnClickListener {
                refreshQuotes()
                refreshDollar()
                refreshCoffeeNews()
                requestWeatherLocation()
            }
        }
        addCard(content, refresh, 16)

        content.addView(text(
            "Fontes: CCCV • Open-Meteo • Google Notícias • cotação USD/BRL. Preços do café são referências e negociações locais podem variar.",
            10f, muted
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(16), dp(5), 0)
        })

        // Atalhos rápidos para as áreas principais
        val shortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(4))
        }
        shortcuts.addView(navItem("☕ Café", quoteSection), LinearLayout.LayoutParams(0, dp(42), 1f))
        shortcuts.addView(navItem("🌦 Clima", weatherSection), LinearLayout.LayoutParams(0, dp(42), 1f))
        shortcuts.addView(navItem("💵 Dólar", dollarSection), LinearLayout.LayoutParams(0, dp(42), 1f))
        shortcuts.addView(navItem("📰 Notícias", newsSection), LinearLayout.LayoutParams(0, dp(42), 1f))
        content.addView(shortcuts, 1)

        // Navegação fixa na parte inferior
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedBackground(panel, panelBorder, 0)
        }
        bottomBar.addView(navItem("☕\nMercado", quoteSection), LinearLayout.LayoutParams(0, dp(54), 1f))
        bottomBar.addView(navItem("🌦\nClima", weatherSection), LinearLayout.LayoutParams(0, dp(54), 1f))
        bottomBar.addView(navItem("💵\nDólar", dollarSection), LinearLayout.LayoutParams(0, dp(54), 1f))
        bottomBar.addView(navItem("📰\nNotícias", newsSection), LinearLayout.LayoutParams(0, dp(54), 1f))
        root.addView(bottomBar)
        val appFrame = FrameLayout(this)
        appFrame.addView(root, FrameLayout.LayoutParams(-1, -1))
        val splash = createSplashOverlay()
        appFrame.addView(splash, FrameLayout.LayoutParams(-1, -1))
        setContentView(appFrame)
        Handler(MainLooper.getMainLooper()).postDelayed({
            if (splash.parent === appFrame) appFrame.removeView(splash)
        }, 1900)
    }

    private fun refreshQuotes() {
        quoteStatus.text = "Consultando a tabela oficial do CCCV…"
        thread {
            try {
                val connection = (URL("https://www.cccv.org.br/cotacao/").openConnection() as HttpURLConnection)
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
                val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()

                fun clean(value: String): String = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
                    .toString().replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim()

                val monthMatch = Regex("(?is)Cotação do café referente ao mês de\\s*([^<\\n]+)").find(raw)
                val monthYear = monthMatch?.groupValues?.get(1)?.let { clean(it).trim('.', ' ', '\r') } ?: "mês atual"
                val rowRegex = Regex("(?is)<tr\\b[^>]*>(.*?)</tr>")
                val cellRegex = Regex("(?is)<t[dh]\\b[^>]*>(.*?)</t[dh]>")
                val priceRegex = Regex("\\d{1,3}(?:\\.\\d{3})*,\\d{2}")
                var latestDay: String? = null
                var dura: String? = null
                var rio: String? = null
                var conilon: String? = null

                for (row in rowRegex.findAll(raw)) {
                    val cells = cellRegex.findAll(row.groupValues[1]).map { clean(it.groupValues[1]) }.toList()
                    if (cells.size >= 4 && cells[0].matches(Regex("\\d{1,2}"))) {
                        val p1 = priceRegex.find(cells[1])?.value
                        val p2 = priceRegex.find(cells[2])?.value
                        val p3 = priceRegex.find(cells[3])?.value
                        if (p1 != null && p2 != null && p3 != null) {
                            latestDay = cells[0]
                            dura = p1
                            rio = p2
                            conilon = p3
                        }
                    }
                }

                if (latestDay == null || dura == null || rio == null || conilon == null) {
                    throw IllegalStateException("A tabela diária do CCCV não retornou as três referências.")
                }

                runOnUiThread {
                    quoteArabica.text = "Arábica • bebida dura   R$ $dura"
                    quoteArabicaRio.text = "Arábica • bebida rio   R$ $rio"
                    quoteConilon.text = "Conilon • tipo 7/8   R$ $conilon"
                    quoteUpdated.text = "CCCV • saca de 60 kg • $latestDay de $monthYear"
                    quoteStatus.text = "Valores oficiais publicados pelo CCCV. Atualizado ao consultar a página."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    quoteStatus.text = "Não foi possível ler a tabela do CCCV agora. Verifique a internet e tente novamente."
                }
            }
        }
    }


    private fun refreshDollar() {
        dollarStatus.text = "Consultando dólar comercial…"
        thread {
            try {
                val connection = URL("https://economia.awesomeapi.com.br/json/last/USD-BRL").openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("User-Agent", "CafeEClima/2.1 Android")
                val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()
                val quote = JSONObject(response).getJSONObject("USDBRL")
                val bid = quote.getString("bid").toDouble()
                val change = quote.optString("pctChange", "0").toDoubleOrNull() ?: 0.0
                val updated = quote.optString("create_date", "")
                val price = "R$ " + String.format(Locale("pt", "BR"), "%.4f", bid)
                val variation = String.format(Locale("pt", "BR"), "%+.2f%%", change)
                runOnUiThread {
                    dollarValue.text = price
                    dollarStatus.text = "Variação: $variation" + if (updated.isNotBlank()) "  •  $updated" else ""
                }
            } catch (_: Exception) {
                runOnUiThread {
                    dollarValue.text = "Cotação indisponível"
                    dollarStatus.text = "Não foi possível consultar o dólar agora. Tente atualizar."
                }
            }
        }
    }

    private data class CoffeeNewsItem(val title: String, val link: String, val date: String)

    private fun refreshCoffeeNews() {
        newsStatus.text = "Buscando notícias recentes…"
        newsBox.removeAllViews()
        thread {
            try {
                val address = "https://news.google.com/rss/search?q=caf%C3%A9+%28conilon+OR+ar%C3%A1bica+OR+mercado%29&hl=pt-BR&gl=BR&ceid=BR%3Apt-419"
                val connection = URL(address).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) CafeEClima/2.1")
                val stream = connection.inputStream
                val parser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                val items = mutableListOf<CoffeeNewsItem>()
                var event = parser.eventType
                var insideItem = false
                var tag = ""
                var title = ""
                var link = ""
                var date = ""
                while (event != XmlPullParser.END_DOCUMENT && items.size < 8) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            tag = parser.name
                            if (tag == "item") {
                                insideItem = true
                                title = ""
                                link = ""
                                date = ""
                            }
                        }
                        XmlPullParser.TEXT -> if (insideItem) {
                            when (tag) {
                                "title" -> title += parser.text
                                "link" -> link += parser.text
                                "pubDate" -> date += parser.text
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "item") {
                                if (title.isNotBlank() && link.isNotBlank()) {
                                    items.add(CoffeeNewsItem(title.trim(), link.trim(), date.trim()))
                                }
                                insideItem = false
                            }
                            tag = ""
                        }
                    }
                    event = parser.next()
                }
                stream.close()
                connection.disconnect()
                runOnUiThread {
                    newsBox.removeAllViews()
                    if (items.isEmpty()) {
                        newsStatus.text = "Nenhuma notícia disponível no momento."
                    } else {
                        newsStatus.text = "Feed de notícias • Google Notícias"
                        items.forEachIndexed { index, entryItem ->
                            val entry = text("•  ${entryItem.title}", 14f, dark, true).apply {
                                setPadding(0, dp(10), 0, dp(4))
                                isClickable = true
                                setOnClickListener {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entryItem.link)))
                                }
                            }
                            newsBox.addView(entry)
                            if (entryItem.date.isNotBlank()) {
                                newsBox.addView(text(entryItem.date, 10f, muted))
                            }
                            if (index < items.lastIndex) {
                                val line = View(this).apply {
                                    setBackgroundColor(panelBorder)
                                    layoutParams = LinearLayout.LayoutParams(-1, dp(1)).apply {
                                        topMargin = dp(8)
                                    }
                                }
                                newsBox.addView(line)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    newsStatus.text = "Não foi possível carregar as notícias. Confira a internet e tente novamente."
                }
            }
        }
    }

    private fun requestWeatherLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            weatherStatus.text = "Autorize a localização para mostrar o clima da sua região."
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionRequest
            )
            return
        }
        getPhoneLocation()
    }

    @Suppress("MissingPermission")
    private fun getPhoneLocation() {
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = locationManager!!.getProviders(true)
            val provider = when {
                providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                providers.isNotEmpty() -> providers[0]
                else -> null
            }
            if (provider == null) {
                weatherStatus.text = "Ative a localização/GPS do celular e tente novamente."
                return
            }
            val last = locationManager!!.getLastKnownLocation(provider)
            if (last != null) loadWeather(last.latitude, last.longitude)
            weatherStatus.text = "Localizando o aparelho…"
            locationManager!!.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    loadWeather(location.latitude, location.longitude)
                    try { locationManager?.removeUpdates(this) } catch (_: Exception) {}
                }
            }, Looper.getMainLooper())
        } catch (e: Exception) {
            weatherStatus.text = "Não foi possível obter o GPS. Confira a permissão e tente atualizar."
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequest) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) getPhoneLocation()
            else weatherStatus.text = "Permissão negada. Ative a localização nas configurações do aplicativo."
        }
    }

    private fun loadWeather(latitude: Double, longitude: Double) {
        weatherStatus.text = "Carregando previsão para sua localização…"
        thread {
            try {
                val endpoint = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum" +
                    "&timezone=auto&forecast_days=7"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection)
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()
                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val daily = json.getJSONObject("daily")
                val temp = current.getDouble("temperature_2m").toInt()
                val feels = current.getDouble("apparent_temperature").toInt()
                val humidity = current.getInt("relative_humidity_2m")
                val wind = current.getDouble("wind_speed_10m").toInt()
                val rainNow = current.optDouble("precipitation", 0.0)
                val code = current.getInt("weather_code")
                val times = daily.getJSONArray("time")
                val codes = daily.getJSONArray("weather_code")
                val maxs = daily.getJSONArray("temperature_2m_max")
                val mins = daily.getJSONArray("temperature_2m_min")
                val probs = daily.getJSONArray("precipitation_probability_max")
                val sums = daily.getJSONArray("precipitation_sum")
                val days = StringBuilder()
                for (i in 0 until times.length()) {
                    val dateRaw = times.getString(i)
                    val date = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateRaw)
                        SimpleDateFormat("EEE dd/MM", Locale("pt", "BR")).format(parsed!!)
                    } catch (_: Exception) { dateRaw }
                    val desc = weatherDescription(codes.getInt(i))
                    days.append("☀  $date   ${mins.getDouble(i).toInt()}° / ${maxs.getDouble(i).toInt()}°\n")
                    days.append("    $desc  •  Chuva: ${probs.optInt(i)}%  •  ${sums.getDouble(i).toInt()} mm\n")
                    if (i < times.length() - 1) days.append("\n")
                }
                runOnUiThread {
                    weatherStatus.text = "Previsão para sua posição • ${"%.4f".format(Locale.US, latitude)}, ${"%.4f".format(Locale.US, longitude)}"
                    currentWeather.text = "${weatherEmoji(code)}  $temp°C  •  ${weatherDescription(code)}\n" +
                        "Sensação: $feels°C   |   Umidade: $humidity%\n" +
                        "Vento: $wind km/h   |   Chuva agora: ${"%.1f".format(Locale("pt", "BR"), rainNow)} mm"
                    forecastBox.removeAllViews()
                    forecastBox.addView(text(days.toString().trim(), 14f, dark))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    weatherStatus.text = "Não foi possível carregar o clima. Confira a internet e tente atualizar."
                }
            }
        }
    }

    private fun weatherDescription(code: Int): String = when (code) {
        0 -> "Céu limpo"
        1 -> "Predominantemente limpo"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Neblina"
        51, 53, 55 -> "Garoa"
        56, 57 -> "Garoa congelante"
        61, 63, 65 -> "Chuva"
        66, 67 -> "Chuva congelante"
        71, 73, 75, 77 -> "Neve"
        80, 81, 82 -> "Pancadas de chuva"
        85, 86 -> "Pancadas de neve"
        95, 96, 99 -> "Trovoadas"
        else -> "Condição variável"
    }

    private fun weatherEmoji(code: Int): String = when (code) {
        0, 1 -> "☀️"
        2, 3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67 -> "🌦️"
        in 71..77, 85, 86 -> "❄️"
        in 80..82 -> "🌧️"
        in 95..99 -> "⛈️"
        else -> "🌤️"
    }
}
