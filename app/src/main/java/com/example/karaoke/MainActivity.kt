package com.example.karaoke

import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

import java.io.IOException

class MainActivity : ComponentActivity() {
    private var startTV: TextView? = null
    private var stopTV: TextView? = null
    private var playTV: TextView? = null
    private var stopplayTV: TextView? = null
    private var statusTV: TextView? = null

    private var mRecorder: MediaRecorder? = null
    private var mPlayer: MediaPlayer? = null
    private var mFileName: String? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        statusTV = findViewById(R.id.idTVstatus)
        startTV = findViewById(R.id.btnRecord)
        stopTV = findViewById(R.id.btnStop)
        playTV = findViewById(R.id.btnPlay)
        stopplayTV = findViewById(R.id.btnStopPlay)

        // Set initial button states
        stopTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        playTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        stopplayTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        // Set up onClick listeners
        startTV?.setOnClickListener { startRecording() }
        stopTV?.setOnClickListener { stopRecording() }
        playTV?.setOnClickListener { playAudio() }
        stopplayTV?.setOnClickListener { stopPlaying() }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startRecording() {
        if (checkPermissions()) {
            // Set the file name and path for the audio file
            mFileName = "${getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath}/AudioRecording.3gp"

            // Initialize MediaRecorder
            mRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(mFileName)
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e("TAG", "Recording prepare() failed: $e")
                    statusTV?.text = "Recording Failed to Start"
                }
                start()
                statusTV?.text = "Recording Started"
                startTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.darker_gray))
                stopTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.holo_purple))
            }
        } else {
            requestPermissions()
        }
    }

    private fun stopRecording() {
        try {
            mRecorder?.apply {
                stop()
                release()
                mRecorder = null
            }
            statusTV?.text = "Recording Stopped"
            stopTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            playTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_purple))
        } catch (e: IllegalStateException) {
            Log.e("TAG", "Stop recording failed: $e")
            statusTV?.text = "Error Stopping Recording"
        }
    }

    private fun playAudio() {
        mPlayer = MediaPlayer().apply {
            try {
                setDataSource(mFileName)
                prepare()
                start()
                statusTV?.text = "Playing Recording"
                playTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.darker_gray))
                stopplayTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.holo_purple))
            } catch (e: IOException) {
                Log.e("TAG", "Play prepare() failed: $e")
                statusTV?.text = "Error Playing Audio"
            }
        }
    }

    private fun stopPlaying() {
        mPlayer?.release()
        mPlayer = null
        statusTV?.text = "Audio Playback Stopped"
        stopplayTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        playTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_purple))
    }

    private fun checkPermissions(): Boolean {
        val audioPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        return audioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_AUDIO_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_AUDIO_PERMISSION_CODE = 1
    }
}