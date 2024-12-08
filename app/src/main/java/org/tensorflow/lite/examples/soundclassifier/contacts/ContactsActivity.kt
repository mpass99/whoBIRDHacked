package org.tensorflow.lite.examples.soundclassifier.contacts

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityInfoBinding

class ContactsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityInfoBinding.inflate(layoutInflater)

        val acceptButton: Button = binding.buttonAccept
        acceptButton.setOnClickListener {
            ContactsService(this).checkForKnownUsers()
        }

        val declineButton: Button = binding.buttonDecline
        declineButton.setOnClickListener {
            finish()
        }
        setContentView(binding.root)
    }
}