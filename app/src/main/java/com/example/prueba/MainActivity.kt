package com.example.prueba

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    val retrofitGoogle = Retrofit.Builder()
        .baseUrl("https://translation.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val traductorApi = retrofitGoogle.create(GoogleTranslateApi::class.java)

    private lateinit var api: OpenFdaApi
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchInput = findViewById(R.id.editTextSearch)
        searchButton = findViewById(R.id.buttonSearch)
        resultText = findViewById(R.id.textViewResults)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.fda.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(OpenFdaApi::class.java)

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                buscarMedicamento(query)
            }
        }
    }

    private fun buscarMedicamento(nombre: String) {
        lifecycleScope.launch {
            try {
                // 🔁 Paso 1: Traducir el nombre del medicamento de español a inglés
                val traduccion = traductorApi.traducirTexto(
                    texto = nombre,
                    idiomaDestino = "en",
                    formato = "text",
                    apiKey = "API_KEY_AQUI"
                )

                val nombreEnIngles = traduccion.body()
                    ?.data
                    ?.translations
                    ?.firstOrNull()
                    ?.translatedText
                    ?: nombre // fallback si falla la traducción

                val response = api.getMedicamentos("openfda.substance_name:$nombreEnIngles", 1)
                val medicamento = response.body()?.results?.firstOrNull()

                if (response.isSuccessful && medicamento != null) {
                    val marca = medicamento.openfda?.brand_name?.joinToString() ?: "Desconocida"
                    val fabricante = medicamento.openfda?.manufacturer_name?.joinToString() ?: "Desconocido"
                    val proposito = medicamento.purpose?.joinToString() ?: "No especificado"
                    val uso = medicamento.indications_and_usage?.joinToString() ?: "No indicado"

                    val textoOriginal = """
                    Brand: $marca
                    Manufacturer: $fabricante
                    Purpose: $proposito
                    Usage: $uso
                """.trimIndent()

                    // Traducción ✨
                    val traduccion = traductorApi.traducirTexto(
                        texto = textoOriginal,
                        idiomaDestino = "es",
                        formato = "text",
                        apiKey = "API_KEY_AQUI"
                    )

                    if (traduccion.isSuccessful) {
                        val textoTraducido = traduccion.body()?.data?.translations?.firstOrNull()?.translatedText
                        resultText.text = textoTraducido ?: textoOriginal
                    } else {
                        resultText.text = textoOriginal
                    }

                } else {
                    resultText.text = "No se encontró información para '$nombre'."
                }

            } catch (e: Exception) {
                resultText.text = "Error: ${e.message}"
                Log.e("API_ERROR", e.toString())
            }
        }
    }
}