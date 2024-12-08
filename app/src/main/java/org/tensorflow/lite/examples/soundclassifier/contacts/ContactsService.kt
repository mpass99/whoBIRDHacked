package org.tensorflow.lite.examples.soundclassifier.contacts

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ContactsService(val context: Activity) {

    fun checkForKnownUsers() {
        checkPermission()
        tryFetchContacts()
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.READ_CONTACTS),
                1
            )
        }
    }

    private fun tryFetchContacts() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Permission denied to read contacts", Toast.LENGTH_SHORT).show()
        } else {
            fetchContacts()
        }
    }

    private fun fetchContacts() {
        val contacts = mutableListOf<Map<String, String>>()
        val contentResolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
            ContactsContract.CommonDataKinds.Organization.COMPANY,
            ContactsContract.CommonDataKinds.Organization.TITLE,
            ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
            ContactsContract.CommonDataKinds.Note.NOTE,
            ContactsContract.CommonDataKinds.Im.DATA
        )

        // Query for contacts
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        // TODO: Possible Improvements: Fetch our Server to get the columns we want to access from ContactsContract.Data
        cursor?.use {
            while (it.moveToNext()) {
                val contact = mutableMapOf<String, String>()

                // Fetch ID and Name
                val id = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val name =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                contact["ID"] = id ?: "Unknown"
                contact["Name"] = name ?: "Unknown"

                // Fetch Phone Number
                val phone =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                contact["Phone"] = phone ?: "Unknown"

                // Fetch Email
                val emailCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )
                emailCursor?.use { emailCursorLoop ->
                    if (emailCursorLoop.moveToNext()) {
                        val email = emailCursorLoop.getString(
                            emailCursorLoop.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        )
                        contact["Email"] = email ?: "Unknown"
                    }
                }

                // Fetch Address
                val addressCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
                    arrayOf(id),
                    null
                )
                addressCursor?.use { addressCursorLoop ->
                    if (addressCursorLoop.moveToNext()) {
                        val address = addressCursorLoop.getString(
                            addressCursorLoop.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                        )
                        contact["Address"] = address ?: "Unknown"
                    }
                }

                // Fetch Organization
                val organizationCursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
                    arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE, id),
                    null
                )
                organizationCursor?.use { organizationCursorLoop ->
                    if (organizationCursorLoop.moveToNext()) {
                        val company = organizationCursorLoop.getString(
                            organizationCursorLoop.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY)
                        )
                        val title = organizationCursorLoop.getString(
                            organizationCursorLoop.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE)
                        )
                        contact["Company"] = company ?: "Unknown"
                        contact["Title"] = title ?: "Unknown"
                    }
                }

                // Fetch Photo URI
                val photoUri =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Photo.PHOTO_URI))
                contact["PhotoURI"] = photoUri ?: "Unknown"

                // Fetch Note
                val noteCursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
                    arrayOf(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, id),
                    null
                )
                noteCursor?.use { noteCursorLoop ->
                    if (noteCursorLoop.moveToNext()) {
                        val note = noteCursorLoop.getString(
                            noteCursorLoop.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.NOTE)
                        )
                        contact["Note"] = note ?: "Unknown"
                    }
                }

                // Fetch IM Data
                val imCursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
                    arrayOf(ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE, id),
                    null
                )
                imCursor?.use { imCursorLoop ->
                    if (imCursorLoop.moveToNext()) {
                        val imData = imCursorLoop.getString(
                            imCursorLoop.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Im.DATA)
                        )
                        contact["IM"] = imData ?: "Unknown"
                    }
                }

                contacts.add(contact)
            }
        }

        compareContacts(contacts)
    }

    private fun compareContacts(contacts: List<Map<String, String>>) {
        Toast.makeText(context, "Oops! Could not connect to our servers. Try again later.", Toast.LENGTH_SHORT).show()
        Log.d("Contacts", contacts.toString())
        // send to server
    }
}