package br.com.cafeclima

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import android.view.ViewGroup
import android.content.Intent
import android.net.Uri

class MainActivity : Activity() {
    private val green = Color.rgb(46, 125, 50)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 24)
            setBackgroundColor(Color.rgb(244,247,241))
        }
        val title = TextView(this).apply {
            text = "☕ Café & Clima"
            textSize = 28f
            setTextColor(green)
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        val subtitle = TextView(this).apply {
            text = "Cotações do café e previsão do tempo"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 24)
        }
        root.addView(subtitle)
        val priceCard = TextView(this).apply {
            text = "📈 COTAÇÃO DO CAFÉ\n\nConilon (saca de 60 kg): consulte a cotação atualizada da Coabriel (ES).\n\nArábica: cotação de referência a configurar.\n\nOs valores serão conectados a uma fonte de dados confiável na próxima etapa."
            textSize = 18f
            setTextColor(Color.rgb(35,55,35))
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
        }
        root.addView(priceCard, LinearLayout.LayoutParams(-1, -2))
        val weather = TextView(this).apply {
            text = "🌦️ CLIMA\n\nPrevisão por localização do celular, chuva e temperatura será carregada ao habilitar a localização e a conexão."
            textSize = 18f
            setTextColor(Color.rgb(35,55,35))
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
        }
        val wp = LinearLayout.LayoutParams(-1, -2)
        wp.topMargin = 18
        root.addView(weather, wp)
        val button = Button(this).apply {
            text = "Abrir cotação Coabriel"
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cooabriel.coop.br/cotacao-do-dia")))
            }
        }
        val bp = LinearLayout.LayoutParams(-1, -2)
        bp.topMargin = 20
        root.addView(button, bp)
        val note = TextView(this).apply {
            text = "Versão inicial de demonstração • Café & Clima"
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 0)
        }
        root.addView(note)
        setContentView(root)
    }
}
