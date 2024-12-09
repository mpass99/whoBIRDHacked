package org.tensorflow.lite.examples.soundclassifier.contacts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.soundclassifier.MainActivity
import org.tensorflow.lite.examples.soundclassifier.R
import org.tensorflow.lite.examples.soundclassifier.SettingsActivity
import org.tensorflow.lite.examples.soundclassifier.ViewActivity
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityContactsBinding

class ContactsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityContactsBinding.inflate(layoutInflater)

        val acceptButton: Button = binding.buttonAccept
        acceptButton.setOnClickListener {
            ContactsService(this).checkForKnownUsers()
            finish()
        }

        val declineButton: Button = binding.buttonDecline
        declineButton.setOnClickListener {
            finish()
        }

        setContentView(binding.root)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_mic -> {
                    intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.action_about -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/woheller69/whobird")))
                }
                R.id.action_view -> {
                    intent = Intent(this, ViewActivity::class.java)
                    startActivity(intent)
                }
                R.id.action_settings -> {
                    intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
    }
}