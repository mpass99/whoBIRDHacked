package org.tensorflow.lite.examples.soundclassifier

import android.Manifest.permission.RECORD_AUDIO
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

private const val TAG = "SOUND_SERVICE"
private const val CHANNEL_ID = "my_channel_01"

class SoundService : Service() {
    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    var isClosed: Boolean = true
        private set

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioRecord: AudioRecord

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "Starting onStartCommand: intent=${intent.hasExtra("sound")}")
        val sound = intent.getIntExtra("sound", R.raw.nordstreifenkiwi)

        val myCompletionCallback = object: OnCompletionListener {
            override fun onCompletion(mp: MediaPlayer) {
                mp.reset()
                mp.release()
                stopRecord()
                stopSelf()
            }
        }

        startAudioPlayback(sound, myCompletionCallback)
        startAudioRecord()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onCreate() {
        Log.i(TAG, "Starting onCreate")
        var microphoneGranted = true
        if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No Audio Permissions")
            microphoneGranted = false
        }

        val channel = NotificationChannel(CHANNEL_ID, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("").build()

        var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        if (microphoneGranted) {
            serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }

        startForeground(1, notification, serviceType)
        Log.i(TAG, "Finishing onCreate")
    }

    override fun onDestroy() {
        Log.i(TAG, "Starting onDestroy")
    }

    fun stopRecord() {
        if (isClosed || !isRecording) return
        if (this::audioRecord.isInitialized) audioRecord.stop()
        isRecording = false
    }

    @Synchronized
    private fun startAudioRecord() {
        Log.d(TAG, "Starting startAudioRecord: isRecording=${isRecording}")
        if (isRecording) return
        setupAudioRecord()
        isClosed = false
        isRecording = true
    }

    @Synchronized
    private fun startAudioPlayback(sound: Int, callback: OnCompletionListener) {
        if (isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer.create(this, sound)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener(callback)
        isPlaying = true
    }

    private fun setupAudioRecord() {
        Log.i(TAG, "Starting setupAudioRecord")
        if (ActivityCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        Log.i(TAG, "Starting Record")
        val sampleRate = 48000
        var bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = sampleRate * 2
        }
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val audioSource = sharedPref.getString("audio_source", MediaRecorder.AudioSource.UNPROCESSED.toString())
        if (audioSource == null) {
            return
        }
        audioRecord = AudioRecord(
            // including MIC, UNPROCESSED, and CAMCORDER.
            Integer.parseInt(audioSource),
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            return
        }

        audioRecord.startRecording()
    }
}