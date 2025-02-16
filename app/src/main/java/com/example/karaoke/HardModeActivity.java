package com.example.karaoke;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.writer.WriterProcessor;

public class HardModeActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_STORAGE_PERMISSION = 201;

    private boolean permissionToRecordAudio = false;
    private boolean permissionToWriteStorage = false;
    private TextView lyricsTextView; // TextView for displaying lyrics
    private Runnable lyricRunnable; // Runnable for updating lyrics
    private boolean isPlaybackPaused = false; // Flag to pause lyrics sync when paused

    AudioDispatcher dispatcher;
    TarsosDSPAudioFormat tarsosDSPAudioFormat;
    File file;
    File referenceFile;  // Reference audio file
    float referencePitch = -1;  // Reference pitch in Hz
    TextView pitchTextView;
    TextView feedbackTextView;  // TextView for feedback
    TextView resultTextView;
    Button recordButton;
    Button switchButton;  // New Switch Button

    private String songTitle = "";
    private String songArtist = "";
    private int songAudioResId = 0;
    private int songLyricsResId = 0;
    private int songVocalId = 0;
    private File recordedAudioFile;
    private boolean isFilePlayed = false;
    // Add counters to track feedback categories
    private int perfectCount = 0;
    private int goodCount = 0;
    private int superCount = 0;
    private int okCount = 0;
    private int pointsCount = 0;


    boolean isRecording = false;
    String filename = "recorded_sound.wav";
    String referenceFilename = "reference_audio.wav";  // The reference audio file

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hard_mode);

        lyricsTextView = findViewById(R.id.lyricsTextView);  // Assuming you have a TextView for lyrics in your layout

        // Retrieve the data passed from MainActivity (or previous activity)
        Intent intent = getIntent();
        if (intent != null) {
            songTitle = intent.getStringExtra("songTitle");
            songArtist = intent.getStringExtra("songArtist");
            songAudioResId = intent.getIntExtra("songAudioResId", 0);
            songLyricsResId = intent.getIntExtra("songLyricsResId", 0);
            songVocalId = intent.getIntExtra("songVocalId", 0);  // Retrieve the vocal ID
        }

        // Request permissions
        requestPermissions();

        // Initialize file paths
        File appDirectory = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "KaraokeApp");
        if (!appDirectory.exists()) {
            appDirectory.mkdirs();  // Create directory if it doesn't exist
        }

        file = new File(appDirectory, filename);
        referenceFile = new File(appDirectory, referenceFilename);

        tarsosDSPAudioFormat = new TarsosDSPAudioFormat(TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
                22050,
                2 * 8,
                1,
                2 * 1,
                22050,
                ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder()));

        pitchTextView = findViewById(R.id.pitchTextView);
        feedbackTextView = findViewById(R.id.feedbackTextView);  // Initialize feedback TextView
        recordButton = findViewById(R.id.recordButton);
        switchButton = findViewById(R.id.btnSwitchMode);  // Initialize Switch Button
        resultTextView = findViewById(R.id.resultTextView);

        // Load reference pitch from the reference audio file
        loadReferencePitch();

        // Record/Stop Button
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    // Reset feedback counters, clear resultTextView, and disable the switchButton
                    resetFeedbackCounters();
                    resultTextView.setText("");  // Clear the feedback summary
                    switchButton.setEnabled(false);  // Disable switch button
                    recordAudio();
                    isRecording = true;
                    recordButton.setText("Evaluate");
                } else {
                    stopRecording();
                    isRecording = false;
                    recordButton.setText("Start");
                    switchButton.setEnabled(true);  // Enable switch button again after recording stops
                }
            }
        });

        // Switch Button
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the new activity (AlternateActivity)
                Intent intent = new Intent(HardModeActivity.this, NormalModeActivity.class);
                intent.putExtra("songTitle", songTitle);
                intent.putExtra("songArtist", songArtist);
                intent.putExtra("songAudioResId", songAudioResId);
                intent.putExtra("songLyricsResId", songLyricsResId);
                intent.putExtra("songVocalId", songVocalId);
                startActivity(intent);
            }
        });

        Button pauseButton = findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaybackPaused = true;  // Pause lyrics sync
                } else {
                    mediaPlayer.start();
                    isPlaybackPaused = false;  // Resume lyrics sync
                }
            }
        });
        // Sync lyrics when starting the song
        if (songLyricsResId != 0) {
            syncLyrics(songLyricsResId);
        }
    }

    private void resetFeedbackCounters() {
        perfectCount = 0;
        goodCount = 0;
        superCount = 0;
        okCount = 0;
        pointsCount = 0;
    }

    // Request permissions
    private void requestPermissions() {
        // Check if audio recording permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionToRecordAudio = true;
        }

        // No need to check storage permissions if using app-specific storage
        permissionToWriteStorage = true; // As Scoped Storage manages this.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAudio = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            permissionToWriteStorage = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }

        if (!permissionToRecordAudio) {
            Toast.makeText(this, "Permission to record audio is required", Toast.LENGTH_SHORT).show();
        }

        if (!permissionToWriteStorage) {
            Toast.makeText(this, "Permission to write to storage is required", Toast.LENGTH_SHORT).show();
        }
    }

    // Load reference pitch from the reference audio file
    private void loadReferencePitch() {
        try {
            if (songVocalId != 0) {
                // Log the resource ID to ensure it's valid
                Log.d("ReferenceFile", "Attempting to load resource with ID: " + songVocalId);

                InputStream referenceInputStream = getResources().openRawResource(songVocalId);

                if (referenceInputStream != null) {
                    Log.d("ReferenceFile", "Reference file loaded successfully.");
                } else {
                    Log.e("ReferenceFile", "Failed to load reference file.");
                }

                // Create an AudioDispatcher using the input stream
                AudioDispatcher referenceDispatcher = new AudioDispatcher(new UniversalAudioInputStream(referenceInputStream, tarsosDSPAudioFormat), 1024, 0);

                // Set up pitch detection handler to detect pitch from the reference audio
                PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
                    @Override
                    public void handlePitch(PitchDetectionResult res, AudioEvent e) {
                        referencePitch = res.getPitch();  // Store the detected pitch
                    }
                };

                // Set up the pitch processor with FFT_YIN algorithm
                AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pitchDetectionHandler);
                referenceDispatcher.addAudioProcessor(pitchProcessor);

                // Start a new thread to run the audio dispatcher
                Thread referenceThread = new Thread(referenceDispatcher, "Reference Audio Thread");
                referenceThread.start();
            } else {
                Log.e("ReferenceFile", "Invalid songVocalId, it is 0.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("ReferenceFile", "Resource not found: " + songVocalId);
        }
    }

    // Compare recorded pitch with reference pitch and provide feedback
    private void comparePitch(float recordedPitch) {
        if (referencePitch == -1) {
            feedbackTextView.setText("Reference pitch not loaded.");
            return;
        }

        if (recordedPitch == -1) {
            feedbackTextView.setText("No pitch detected.");
            return;
        }

        // Calculate the difference in Hz between the recorded pitch and the reference pitch
        float pitchDifference = Math.abs(recordedPitch - referencePitch);

        // Provide feedback based on the pitch difference
        String feedbackMessage = getPitchFeedback(pitchDifference);

        // Update counters based on feedback
        if (feedbackMessage.equals("Perfect!")) {
            pointsCount = pointsCount + 20;
            perfectCount++;
        } else if (feedbackMessage.equals("Good!")) {
            pointsCount = pointsCount + 10;
            goodCount++;
        } else if (feedbackMessage.equals("Super!")) {
            pointsCount = pointsCount + 5;
            superCount++;
        } else if (feedbackMessage.equals("Ok")) {
            pointsCount = pointsCount + 1;
            okCount++;
        }
        feedbackTextView.setText(feedbackMessage);
    }

    private String getPitchFeedback(float pitchDifference) {
        // Provide feedback based on pitch difference
        if (pitchDifference <= 5) {
            return "Perfect!"; // Pitch is within 2 Hz, considered perfect
        } else if (pitchDifference <= 10) {
            return "Super!"; // Pitch difference is small, considered good
        } else if (pitchDifference <= 20) {
            return "Good!"; // Pitch difference is moderate, considered super
        } else {
            return "Ok"; // Pitch difference is large, considered ok
        }
    }


    private void deleteRecordedFile() {
        if (file.exists()) {
            Log.d("RecordPlayActivity", "File exists: " + file.getAbsolutePath());
            boolean deleted = file.delete();
            if (deleted) {
                Toast.makeText(this, "Recorded file deleted after playback.", Toast.LENGTH_SHORT).show();
                Log.d("RecordPlayActivity", "File deleted successfully.");
            } else {
                Toast.makeText(this, "Failed to delete the recorded file.", Toast.LENGTH_SHORT).show();
                Log.e("RecordPlayActivity", "Failed to delete the file.");
            }
        } else {
            Log.e("RecordPlayActivity", "File does not exist: " + file.getAbsolutePath());
        }
    }

    // Record Audio
    public void recordAudio() {
        releaseDispatcher();

        // Start playing the original song in the background
        playOriginalSong();

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);

        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            AudioProcessor recordProcessor = new WriterProcessor(tarsosDSPAudioFormat, randomAccessFile);
            dispatcher.addAudioProcessor(recordProcessor);

            // Pitch Detection Handler during recording
            PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e) {
                    final float recordedPitch = res.getPitch();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pitchTextView.setText("Pitch: " + recordedPitch + " Hz");
                            comparePitch(recordedPitch);  // Compare pitch with reference
                        }
                    });
                }
            };

            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pitchDetectionHandler);
            dispatcher.addAudioProcessor(pitchProcessor);

            Thread audioThread = new Thread(dispatcher, "Audio Thread");
            audioThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    MediaPlayer mediaPlayer;

    public void playOriginalSong() {
        // Initialize MediaPlayer to play the song
        try {
            mediaPlayer = MediaPlayer.create(this, songAudioResId);
            mediaPlayer.setLooping(true);  // Optional: Set looping if desired
            mediaPlayer.start();

            // Start lyrics sync when the song begins
            if (songLyricsResId != 0) {
                syncLyrics(songLyricsResId);  // Start syncing lyrics
            }
        } catch (Exception e) {
            Log.e("SongPlay", "Error playing the song: " + e.getMessage());
        }
    }

    // Play recorded audio
    public void playAudio() {
        try {
            releaseDispatcher();

            FileInputStream fileInputStream = new FileInputStream(file);
            dispatcher = new AudioDispatcher(new UniversalAudioInputStream(fileInputStream, tarsosDSPAudioFormat), 1024, 0);

            AudioProcessor playerProcessor = new AndroidAudioPlayer(tarsosDSPAudioFormat, 2048, 0);
            dispatcher.addAudioProcessor(playerProcessor);

            PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e) {
                    final float recordedPitch = res.getPitch();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pitchTextView.setText("Pitch: " + recordedPitch + " Hz");
                            comparePitch(recordedPitch);  // Compare pitch with reference
                        }
                    });
                }
            };

            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pitchDetectionHandler);
            dispatcher.addAudioProcessor(pitchProcessor);

            Thread audioThread = new Thread(dispatcher, "Audio Thread");
            audioThread.start();

            // Set flag to indicate the file has been played
            isFilePlayed = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Stop recording
    public void stopRecording() {
        releaseDispatcher();
        mediaPlayer.stop();
        showFeedbackSummary();
    }

    // Release resources
    public void releaseDispatcher() {
        if (dispatcher != null) {
            if (!dispatcher.isStopped()) {
                dispatcher.stop();
            }
            dispatcher = null;
        }
    }
    private void showFeedbackSummary() {
        // Create a summary string
        String summary = "Feedback Summary:\n" +
                "Perfect: " + perfectCount + "\n" +
                "Super: " + superCount + "\n" +
                "Good: " + goodCount + "\n" +
                "Ok: " + okCount + "\n" +
                "Points: " + pointsCount;

        resultTextView.setText(summary);
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseDispatcher();
        // If the file was played, delete the recorded file
        if (isFilePlayed) {
            deleteRecordedFile();
        }
    }
    private void syncLyrics(int lyricsResId) {
        final List<Pair<Double, String>> lyrics = loadLyricsFromFile(lyricsResId);
        final Handler handler = new Handler(Looper.getMainLooper());

        lyricRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && !isPlaybackPaused) {
                    double currentPositionInSeconds = mediaPlayer.getCurrentPosition() / 1000.0;

                    // Iterate through the lyrics and display the correct one based on the time
                    for (Pair<Double, String> lyric : lyrics) {
                        if (currentPositionInSeconds >= lyric.first) {
                            lyricsTextView.setText(lyric.second);
                        }
                    }
                }
                handler.postDelayed(this, 100); // Update every 100ms
            }
        };

        // Start syncing lyrics
        handler.post(lyricRunnable);
    }

    private List<Pair<Double, String>> loadLyricsFromFile(int lyricsResId) {
        List<Pair<Double, String>> lyrics = new ArrayList<>();
        try {
            InputStream inputStream = getResources().openRawResource(lyricsResId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            // Parse each line in the format [mm:ss.xx] Lyric
            while ((line = reader.readLine()) != null) {
                // Regex to match time and lyric format
                Pattern pattern = Pattern.compile("\\[(\\d+):(\\d+\\.\\d+)\\](.*)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    double seconds = Double.parseDouble(matcher.group(2));
                    String lyric = matcher.group(3).trim();

                    double timestamp = minutes * 60 + seconds;  // Convert to seconds
                    lyrics.add(new Pair<>(timestamp, lyric));
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e("Lyrics", "Error loading lyrics file", e);
        }
        return lyrics;
    }


}
