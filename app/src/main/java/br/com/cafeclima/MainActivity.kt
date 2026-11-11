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
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.content.Intent
import android.net.Uri
import android.text.Html
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val green = Color.rgb(38, 91, 57)
    private val dark = Color.rgb(34, 48, 39)
    private val muted = Color.rgb(104, 119, 108)
    private val background = Color.rgb(242, 246, 239)
    private lateinit var content: LinearLayout
    private lateinit var quoteStatus: TextView
    private lateinit var weatherStatus: TextView
    private lateinit var currentWeather: TextView
    private lateinit var forecastBox: LinearLayout
    private lateinit var quoteArabica: TextView
    private lateinit var quoteConilon: TextView
    private lateinit var quoteUpdated: TextView
    private var locationManager: LocationManager? = null
    private val locationPermissionRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        refreshQuotes()
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

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundColor(Color.WHITE)
            elevation = dp(2).toFloat()
        }
    }

    private fun addCard(parent: LinearLayout, child: View, top: Int = 12) {
        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(top)
        parent.addView(child, params)
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply { setBackgroundColor(background) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(28))
        }
        scroll.addView(content)

        content.addView(text("☕ Café & Clima", 28f, green, true).apply {
            gravity = Gravity.CENTER
        })
        content.addView(text("Mercado do café e clima na sua região", 14f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(12))
        })

        val quoteCard = card()
        quoteCard.addView(text("📈  COTAÇÃO DO CAFÉ", 17f, green, true))
        quoteCard.addView(text("Indicadores nacionais • saca de 60 kg", 12f, muted).apply {
            setPadding(0, dp(4), 0, dp(12))
        })
        quoteArabica = text("Arábica: consultando…", 20f, dark, true)
        quoteConilon = text("Conilon: consultando…", 20f, dark, true)
        quoteCard.addView(quoteArabica)
        quoteCard.addView(quoteConilon.apply { setPadding(0, dp(8), 0, 0) })
        quoteUpdated = text("Fonte: Cotação do Café (indicador nacional informado como Cepea/Esalq)", 11f, muted)
        quoteUpdated.setPadding(0, dp(10), 0, 0)
        quoteCard.addView(quoteUpdated)
        quoteStatus = text("Buscando preços pela internet…", 12f, muted)
        quoteStatus.setPadding(0, dp(6), 0, 0)
        quoteCard.addView(quoteStatus)
        val quoteButton = Button(this).apply {
            text = "Ver cotação e histórico"
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cotacaodocafe.com/")))
            }
        }
        quoteCard.addView(quoteButton)
        addCard(content, quoteCard, 10)

        val weatherCard = card()
        weatherCard.addView(text("🌦️  CLIMA NA SUA LOCALIZAÇÃO", 17f, green, true))
        weatherStatus = text("Obtendo localização do celular…", 13f, muted)
        weatherStatus.setPadding(0, dp(5), 0, dp(8))
        weatherCard.addView(weatherStatus)
        currentWeather = text("Aguardando previsão…", 16f, dark)
        currentWeather.setPadding(0, 0, 0, dp(12))
        weatherCard.addView(currentWeather)
        weatherCard.addView(text("PRÓXIMOS 7 DIAS", 14f, green, true).apply {
            setPadding(0, dp(4), 0, dp(8))
        })
        forecastBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        weatherCard.addView(forecastBox)
        addCard(content, weatherCard)

        val refresh = Button(this).apply {
            text = "↻  Atualizar cotações e previsão"
            setOnClickListener {
                refreshQuotes()
                requestWeatherLocation()
            }
        }
        addCard(content, refresh, 16)

        content.addView(text(
            "Clima: Open-Meteo. Cotações: indicador nacional de referência; o valor negociado pode variar por região, qualidade, classificação e negociação local.",
            11f, muted
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(18), dp(4), 0)
        })
        setContentView(scroll)
    }

    private fun refreshQuotes() {
        quoteStatus.text = "Atualizando preços…"
        thread {
            try {
                val connection = (URL("https://www.cotacaodocafe.com/").openConnection() as HttpURLConnection)
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.setRequestProperty("User-Agent", "CafeClimaAndroid/1.0")
                val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()
                val plain = if (android.os.Build.VERSION.SDK_INT >= 24) {
                    Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(raw).toString()
                }.replace('\u00a0', ' ')
                val arabica = Regex("(?is)Arábica\\s*R\\x24\\s*([0-9.]+,[0-9]{2})").find(plain)?.groupValues?.get(1)
                val conilon = Regex("(?is)Conilon\\s*R\\x24\\s*([0-9.]+,[0-9]{2})").find(plain)?.groupValues?.get(1)
                val update = Regex("(?is)Atualizado\\s+Hoje\\s+às\\s+([0-9]{1,2}:[0-9]{2})").find(plain)?.groupValues?.get(1)
                runOnUiThread {
                    if (arabica != null && conilon != null) {
                        quoteArabica.text = "Arábica  R$ $arabica"
                        quoteConilon.text = "Conilon  R$ $conilon"
                        quoteStatus.text = "Consulta realizada: \${update ?: "horário não informado"}"
                    } else {
                        quoteArabica.text = "Arábica: indisponível"
                        quoteConilon.text = "Conilon: indisponível"
                        quoteStatus.text = "A fonte não apresentou os valores em formato reconhecido. Toque em “Ver cotação e histórico”."
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    quoteStatus.text = "Sem conexão para atualizar as cotações. Tente novamente."
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
                    days.append("☀  $date   \${mins.getDouble(i).toInt()}° / \${maxs.getDouble(i).toInt()}°\n")
                    days.append("    $desc  •  Chuva: \${probs.optInt(i)}%  •  \${sums.getDouble(i).toInt()} mm\n")
                    if (i < times.length() - 1) days.append("\n")
                }
                runOnUiThread {
                    weatherStatus.text = "Previsão para sua posição • \${"%.4f".format(Locale.US, latitude)}, \${"%.4f".format(Locale.US, longitude)}"
                    currentWeather.text = "\${weatherEmoji(code)}  $temp°C  •  \${weatherDescription(code)}\n" +
                        "Sensação: $feels°C   |   Umidade: $humidity%\n" +
                        "Vento: $wind km/h   |   Chuva agora: \${"%.1f".format(Locale("pt", "BR"), rainNow)} mm"
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
