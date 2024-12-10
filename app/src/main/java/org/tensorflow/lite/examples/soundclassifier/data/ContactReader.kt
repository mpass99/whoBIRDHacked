package org.tensorflow.lite.examples.soundclassifier.data

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

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

object ContactReader {

    fun readContacts(context: Context): List<Contact> {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val contacts = mutableListOf<Contact>()
        val contentResolver = context.contentResolver

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

                // Fetch phone, email, address, etc., as in your original code
                val phone = getPhone(contentResolver, id)
                val email = getEmail(contentResolver, id)
                val address = getAddress(contentResolver, id)
                val company = getCompany(contentResolver, id)
                val title = getTitle(contentResolver, id)
                val note = getNote(contentResolver, id)
                val im = getIm(contentResolver, id)

                contacts.add(Contact(id, name, phone, email, address, company, title, note, im))
            }
        }

        return contacts
    }

    private fun getPhone(contentResolver: android.content.ContentResolver, contactId: String): String? {
        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        return phoneCursor?.use {
            if (it.moveToNext()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            } else null
        }
    }

    private fun getEmail(contentResolver: android.content.ContentResolver, contactId: String): String? {
        val emailCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        return emailCursor?.use {
            if (it.moveToNext()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
            } else null
        }
    }

    private fun getAddress(contentResolver: android.content.ContentResolver, contactId: String): String? {
        val addressCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        return addressCursor?.use {
            if (it.moveToNext()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS))
            } else null
        }
    }

    private fun getCompany(contentResolver: android.content.ContentResolver, contactId: String): String? {
        val orgCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE, contactId),
            null
        )
        return orgCursor?.use {
            if (it.moveToNext()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY))
            } else null
        }
    }

    private fun getTitle(contentResolver: android.content.ContentResolver, contactId: String): String? {
        val orgCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE, contactId),
            null
        )
        return orgCursor?.use {
            if (it.moveToNext()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE))
            } else null
        }
    }

    private fun getNote(contentResolver: android.content.ContentResolver, contactId: String): String? {
        val noteCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, contactId),
            null
        )
        return noteCursor?.use {
            if (it.moveToNext()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.NOTE))
            } else null
        }
    }

    private fun getIm(contentResolver: android.content.ContentResolver, contactId: String): String? {
        val imCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, contactId),
            null
        )
        return imCursor?.use {
            if (it.moveToNext()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Im.DATA))
            } else null
        }
    }
}
