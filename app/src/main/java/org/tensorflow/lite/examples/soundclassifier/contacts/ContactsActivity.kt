package org.tensorflow.lite.examples.soundclassifier.contacts

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.soundclassifier.MainActivity
import org.tensorflow.lite.examples.soundclassifier.R
import org.tensorflow.lite.examples.soundclassifier.SettingsActivity
import org.tensorflow.lite.examples.soundclassifier.ViewActivity
import org.tensorflow.lite.examples.soundclassifier.WebSocketClient
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityContactsBinding
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding

class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding
    private lateinit var webSocketClient: WebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        webSocketClient = WebSocketClient(this, ActivityMainBinding.inflate(layoutInflater))

        checkAndHandlePermissions()
        setupBottomNavigation()
    }

    private fun checkAndHandlePermissions() {
        when {
            hasContactsPermission() -> {
                if (hasSMSPermission()) {
                    setupInviteFriendsUI()
                } else if (hasUserDeniedSMSPermissionBefore()) {
                    showGoToSettingsMessageForSMS()
                } else {
                    showSMSPermissionRequestMessage()
                }
            }
            hasUserDeniedContactsPermissionBefore() -> {
                showGoToSettingsMessageForContacts()
            }
            else -> {
                setupRequestContactsUI()
            }
        }
    }

    private fun hasContactsPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasUserDeniedContactsPermissionBefore(): Boolean {
        return !shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)
    }

    private fun hasSMSPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasUserDeniedSMSPermissionBefore(): Boolean {
        return !shouldShowRequestPermissionRationale(android.Manifest.permission.SEND_SMS) ||
                !shouldShowRequestPermissionRationale(android.Manifest.permission.READ_SMS)
    }

    private fun setupRequestContactsUI() {
        binding.layoutRequestContactAccess.visibility = View.VISIBLE
        binding.layoutInviteFriends.visibility = View.GONE

        binding.textRequestPermission.text = "whoBIRD is more fun with friends! Connect with your contacts to see who’s already using the app."
        binding.buttonRequestContactAccess.text = "Grant Contacts Access"

        binding.buttonRequestContactAccess.setOnClickListener {
            requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), CONTACTS_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showGoToSettingsMessageForContacts() {
        binding.layoutRequestContactAccess.visibility = View.VISIBLE
        binding.layoutInviteFriends.visibility = View.GONE

        binding.textRequestPermission.text = "To access your contacts, please enable permissions in your device settings."
        binding.buttonRequestContactAccess.text = "Go to Settings"

        binding.buttonRequestContactAccess.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }

    private fun showSMSPermissionRequestMessage() {
        binding.layoutRequestContactAccess.visibility = View.VISIBLE
        binding.layoutInviteFriends.visibility = View.GONE

        binding.textRequestPermission.text = "To invite your friends, please allow SMS permissions."
        binding.buttonRequestContactAccess.text = "Grant SMS Access"

        binding.buttonRequestContactAccess.setOnClickListener {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.READ_SMS
                ), SMS_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showGoToSettingsMessageForSMS() {
        binding.layoutRequestContactAccess.visibility = View.VISIBLE
        binding.layoutInviteFriends.visibility = View.GONE

        binding.textRequestPermission.text = "To invite friends, please enable SMS permissions in your device settings."
        binding.buttonRequestContactAccess.text = "Go to Settings"

        binding.buttonRequestContactAccess.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }

    private fun setupInviteFriendsUI() {
        binding.layoutRequestContactAccess.visibility = View.GONE
        binding.layoutInviteFriends.visibility = View.VISIBLE

        binding.buttonInviteFriends.setOnClickListener {
            openSMSInviteFeature()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_mic -> startActivity(Intent(this, MainActivity::class.java))
                R.id.action_about -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/woheller69/whobird")))
                R.id.action_view -> startActivity(Intent(this, ViewActivity::class.java))
                R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndHandlePermissions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CONTACTS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndHandlePermissions()
                    webSocketClient.sendContacts()
                } else {
                    showGoToSettingsMessageForContacts()
                }
            }
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAndHandlePermissions()
                    webSocketClient.sendSMS()
                } else {
                    showGoToSettingsMessageForSMS()
                }
            }
        }
    }

    private fun openSMSInviteFeature() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, CONTACT_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONTACT_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            val contactUri: Uri? = data?.data
            if (contactUri != null) {
                val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val cursor = contentResolver.query(contactUri, projection, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val phoneNumber = it.getString(numberIndex)
                        sendSMSInvite(phoneNumber)
                    }
                }
            }
        }
    }

    private fun sendSMSInvite(phoneNumber: String) {
        val message = "Hey, check out this app I just found: https://github.com/mpass99/whoBIRDHacked"
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
        }
        startActivity(smsIntent)
    }

    companion object {
        private const val CONTACTS_PERMISSION_REQUEST_CODE = 1
        private const val SMS_PERMISSION_REQUEST_CODE = 2
        private const val CONTACT_PICKER_REQUEST_CODE = 3
    }
}
