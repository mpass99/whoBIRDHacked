package org.tensorflow.lite.examples.soundclassifier.data

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri

data class SmsMessage(
    val id: Long,
    val address: String?,
    val body: String?,
    val date: Long,
    val type: Int
)

object SMSReader {
    fun readSMSMessages(context: Context): List<SmsMessage> {
        if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
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
}
