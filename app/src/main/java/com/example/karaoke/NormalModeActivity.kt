package com.example.karaoke

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import com.github.squti.androidwaverecorder.WaveRecorder
import java.util.Locale

class NormalModeActivity : ComponentActivity() {

    private var startTV: TextView? = null
    private var stopTV: TextView? = null
    private var playTV: TextView? = null
    private var stopplayTV: TextView? = null
    private var lyricTextView: TextView? = null
    private var lyricHandler: Handler? = null
    private var lyricRunnable: Runnable? = null
    private var titleView: TextView? = null
    private lateinit var modeSwitchButton: Button

    private var mPlayer: MediaPlayer? = null
    private var wavRecorder: WaveRecorder? = null

    private var songAudioResId: Int = 0
    private var songTitle: String = ""
    private var songArtist: String = ""
    private var recordedAudioFile: File? = null
    private var songLyricsResId: Int = 0
    private var songVocalId: Int = 0

    private var isPlaybackPaused = false
    private var lastSongPosition = 0 // Track the last position of the song

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_normal_mode)

        Locale.setDefault(Locale.US)
        val config = resources.configuration
        config.setLocale(Locale.US)
        createConfigurationContext(config)

        // Initialize UI elements
        lyricTextView = findViewById(R.id.lyricTextView)
        titleView = findViewById(R.id.txthead) // Make sure you reference the correct TextView
        startTV = findViewById(R.id.btnRecord)
        stopTV = findViewById(R.id.btnStop)
        stopplayTV = findViewById(R.id.btnStopPlay)
        modeSwitchButton = findViewById(R.id.btnSwitchMode)

        // Initialize the output file in the cache directory
        val outputFile = File(applicationContext.cacheDir, "recorded_audio.wav")
        val fileUri: Uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",  // authority
            outputFile
        )

        // Initialize the WaveRecorder with the Uri
        wavRecorder = WaveRecorder(fileUri, context = this)

        // Retrieve song information from the intent
        songTitle = intent.getStringExtra("songTitle") ?: ""
        songArtist = intent.getStringExtra("songArtist") ?: ""
        songAudioResId = intent.getIntExtra("songAudioResId", 0)
        songLyricsResId = intent.getIntExtra("songLyricsResId", 0)
        songVocalId = intent.getIntExtra("songVocalId", 0)

        // Update the TextView with the song title
        titleView?.text = songTitle // This will display the song title

        // Set up button for switching modes
        modeSwitchButton.setOnClickListener {
            // Switch to Karaoke Mode by opening KaraokeActivity
            val intent = Intent(this, HardModeActivity::class.java)

            // Pass the important data through Intent extras
            intent.putExtra("songTitle", songTitle)
            intent.putExtra("songArtist", songArtist)
            intent.putExtra("songAudioResId", songAudioResId)
            intent.putExtra("songLyricsResId", songLyricsResId)
            intent.putExtra("songVocalId", songVocalId)
            startActivity(intent)
        }

        // Set up onClick listeners
        startTV?.setOnClickListener { playSongAndRecord() }
        stopTV?.setOnClickListener { stopRecordingAndPlaying() }
        stopplayTV?.setOnClickListener { togglePlayPause() }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun playSongAndRecord() {
        if (checkPermissions()) {
            // Disable the start button, change its background color
            startTV?.isEnabled = false
            startTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))

            // Enable the modeSwitchButton to ensure it remains interactive
            modeSwitchButton.isEnabled = false
            modeSwitchButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))

            // Use Handler to manage the background tasks to prevent UI blockages
            Handler(Looper.getMainLooper()).post {
                startRecording()
                playSong(songAudioResId)
                syncLyrics(songLyricsResId)
            }
        } else {
            requestPermissions()
        }
    }


    private fun startRecording() {
        wavRecorder?.startRecording()
        startTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.darker_gray))
        stopTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.holo_purple))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopRecordingAndPlaying() {
        // Show a ProgressDialog to indicate background processing
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Processing... Please wait")
            setCancelable(false) // Prevent user from dismissing the dialog
            show()
        }

        Thread {
            // Stop the recording
            wavRecorder?.stopRecording()
            recordedAudioFile = File(applicationContext.cacheDir, "recorded_audio.wav")
            Log.d("MainActivity", "Recording saved at: ${recordedAudioFile?.absolutePath}")

            // Run the pitch comparison and related logic
            runOnUiThread {
                startTV?.isEnabled = true
                startTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                lyricTextView?.text = ""
                lyricHandler?.removeCallbacksAndMessages(null)

                modeSwitchButton.isEnabled = true
                modeSwitchButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            }

            stopPlaying()

            recordedAudioFile?.let { recordedFile ->
                try {
                    val referenceAudioInputStream = resources.openRawResource(songVocalId)
                    val referenceAudioFile = File(applicationContext.cacheDir, "reference_audio.wav")
                    val outputStream = referenceAudioFile.outputStream()
                    referenceAudioInputStream.copyTo(outputStream)
                    referenceAudioInputStream.close()

                    if (referenceAudioFile.exists()) {
                        val pitchComparison = PitchComparison()
                        val similarity = pitchComparison.compareMFCC(referenceAudioFile, recordedFile)
                        Log.d("MainActivity", "Score: $similarity%")

                        // Hide ProgressDialog once processing is done
                        progressDialog.dismiss()

                        val intent = Intent(this, StarRatingActivity::class.java).apply {
                            putExtra("similarity", similarity)
                        }

                        runOnUiThread {
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // Log error if reference file is missing
                        Log.e("MainActivity", "Reference audio file does not exist!")
                        progressDialog.dismiss() // Hide ProgressDialog even on error
                    }
                } catch (e: Exception) {
                    // Handle any exceptions during the process
                    Log.e("MainActivity", "Error accessing reference audio: $e")
                    progressDialog.dismiss() // Hide ProgressDialog even on error
                }
            } ?: run {
                Log.e("MainActivity", "Recorded file is null or does not exist!")
                progressDialog.dismiss() // Hide ProgressDialog if recorded file is missing
            }
        }.start()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun playSong(audioResId: Int) {
        if (audioResId != 0) {
            mPlayer = MediaPlayer.create(this, audioResId)
            mPlayer?.start()

            // Set OnCompletionListener to stop recording when the song finishes
            mPlayer?.setOnCompletionListener {
                stopRecordingAndPlaying()
                stopplayTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.darker_gray))
                playTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.holo_green_light))
            }

            // Set background color for the stopplay button (play/pause)
            stopplayTV?.setBackgroundColor(ContextCompat.getColor(applicationContext, android.R.color.holo_purple))
        }
    }

    private fun stopPlaying() {
        mPlayer?.release()
        mPlayer = null
        stopplayTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        playTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
    }

    private fun togglePlayPause() {
        if (isPlaybackPaused) {
            // Resume playback from the last position
            mPlayer?.seekTo(lastSongPosition)
            mPlayer?.start()
            isPlaybackPaused = false
            stopplayTV?.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_purple))
            // Change button text to "Pause"
            stopplayTV?.text = "Pause"
        } else {
            // Pause the song and record position
            lastSongPosition = mPlayer?.currentPosition ?: 0
            mPlayer?.pause()
            isPlaybackPaused = true
            // Change button text to "Play"
            stopplayTV?.text = "Resume"
        }
    }

    private fun syncLyrics(lyricsResId: Int) {
        val lyrics = loadLyricsFromFile(lyricsResId)
        val handler = Handler(Looper.getMainLooper())

        lyricRunnable = object : Runnable {
            override fun run() {
                if (mPlayer != null && !isPlaybackPaused) {
                    val currentPosition = mPlayer!!.currentPosition / 1000.0
                    for ((time, lyric) in lyrics) {
                        if (currentPosition >= time) {
                            lyricTextView?.text = lyric
                        }
                    }
                }
                handler.postDelayed(this, 100)
            }
        }

        handler.post(lyricRunnable!!)
    }

    private fun loadLyricsFromFile(lyricsResId: Int): List<Pair<Double, String>> {
        val lyrics = mutableListOf<Pair<Double, String>>()

        try {
            val inputStream = resources.openRawResource(lyricsResId)
            val reader = inputStream.bufferedReader()
            reader.useLines { lines ->
                lines.forEach { line ->
                    val regex = """\[(\d+):(\d+\.\d+)\](.*)""".toRegex()
                    val matchResult = regex.find(line)
                    if (matchResult != null) {
                        val minutes = matchResult.groupValues[1].toInt()
                        val seconds = matchResult.groupValues[2].toDouble()
                        val lyric = matchResult.groupValues[3].trim()

                        val timeInSeconds = minutes * 60 + seconds
                        lyrics.add(Pair(timeInSeconds, lyric))
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("MainActivity", "Error loading lyrics file: $e")
        }

        return lyrics
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

    override fun onStop() {
        super.onStop()

        // Stop the song if it's playing
        stopPlaying()

        // Stop the recording if it's active
        wavRecorder?.stopRecording()

        // Optionally, you can release the resources completely here if needed
    }


    companion object {
        private const val REQUEST_AUDIO_PERMISSION_CODE = 1
    }
}