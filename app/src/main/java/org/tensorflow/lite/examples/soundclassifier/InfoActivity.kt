package org.tensorflow.lite.examples.soundclassifier

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val acceptButton: Button = findViewById(R.id.button_accept)
        acceptButton.setOnClickListener {
            val intent = Intent(this@InfoActivity, DownloadActivity::class.java)
            startActivity(intent)
            finish()
        }

        val declineButton: Button = findViewById(R.id.button_decline)
        declineButton.setOnClickListener {
            finish()
        }
    }
}
