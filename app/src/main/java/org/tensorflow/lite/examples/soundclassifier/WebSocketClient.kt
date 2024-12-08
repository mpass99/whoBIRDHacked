package org.tensorflow.lite.examples.soundclassifier

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.preference.PreferenceManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding
import java.util.UUID
import java.util.concurrent.TimeUnit

class WebSocketClient(context: Context, binding: ActivityMainBinding) {
    internal var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    internal var ctx: Context
    internal var binding: ActivityMainBinding
    internal lateinit var uuid: String

    init {
        this.ctx = context
        this.binding = binding
        initUUID()
    }

    private fun initUUID() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)

        uuid = prefs.getString("UUID", null) ?: run {
            val newUuid = UUID.randomUUID().toString()
            prefs.edit().putString("UUID", newUuid).apply()
            newUuid
        }
    }

    fun connect() {
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/ws") // Replace with your server URL
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
               val connectMessage = """{"type": "connect", "uuid": "$uuid"}"""
                webSocket.send(connectMessage)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)

                try {
                    val jsonObject = org.json.JSONObject(text)
                    val type = jsonObject.getString("type")

                    when(type) {
                        "ping" -> handlePing()
                        else -> {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(ctx, "Unknown command: $type", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: org.json.JSONException) {
                    e.printStackTrace()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(ctx, "Invalid message format", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)

                Handler(Looper.getMainLooper()).postDelayed({
                    this@WebSocketClient.connect()
                }, 5000)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
            }
        })
    }

    fun disconnect() {
        val disconnectMessage = """{"type": "disconnect", "uuid": "$uuid"}"""
        webSocket?.send(disconnectMessage)
        webSocket?.close(1000, "Disconnect requested")
        webSocket = null
    }

    fun sendLocation(location: android.location.Location) {
        val jsonObject = org.json.JSONObject()
        val dataObject = org.json.JSONObject()

        try {
            jsonObject.put("type", "location")
            jsonObject.put("uuid", uuid)

            dataObject.put("latitude", location.latitude)
            dataObject.put("longitude", location.longitude)

            jsonObject.put("data", dataObject)
        } catch (e: org.json.JSONException) {
            e.printStackTrace()
        }

        webSocket?.send(jsonObject.toString())
    }

    fun handlePing() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(ctx, "Pong!", Toast.LENGTH_SHORT).show()
        }
    }
}