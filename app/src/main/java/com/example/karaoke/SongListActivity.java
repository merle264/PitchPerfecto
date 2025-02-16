package com.example.karaoke;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;  // Use AppCompatActivity instead of ComponentActivity
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

public class SongListActivity extends AppCompatActivity {  // Changed to AppCompatActivity
    private ListView songListView;
    private ArrayList<Song> songList;
    private SongAdapter songAdapter;

    @RequiresApi(Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);

        songListView = findViewById(R.id.songListView);

        // Sample song list with correct resource IDs for audio and lyrics
        songList = new ArrayList<>();
        songList.add(new Song("Angels In Amplifiers: ‘I’m Alright’", "unknown", R.drawable.ic_music_note, R.raw.angels, R.raw.angeisinamplifiersimalrightlyrics, R.raw.angelsvocals));
        songList.add(new Song("Somebody", "Nexto", R.drawable.ic_music_note, R.raw.nextosomebody, R.raw.louderthantheliarlyrics, R.raw.louderthantheliarvocals));
        songList.add(new Song(" Louder Than the Liar_ MAGA Trump Cult Liars", "John Lopker", R.drawable.ic_music_note, R.raw.louderthantheliar, R.raw.nexttosomebodylyrics, R.raw.nexttosomebodyvocals));

        songAdapter = new SongAdapter(this, songList);
        songListView.setAdapter(songAdapter);

        songListView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Song selectedSong = songList.get(position);
            Toast.makeText(SongListActivity.this, "Selected: " + selectedSong.getTitle(), Toast.LENGTH_SHORT).show();

            // Get the audio and lyrics resource IDs
            int songAudioResId = selectedSong.getAudioResourceId();
            int songLyricsResId = selectedSong.getLyricsResourceId();  // Get the lyrics resource ID
            int songVocalId = selectedSong.getVocalResourceId();

            // Pass both audio and lyrics resource IDs to MainActivity2
            Intent intent = new Intent(SongListActivity.this, NormalModeActivity.class);
            intent.putExtra("songTitle", selectedSong.getTitle());  // Pass song title
            intent.putExtra("songArtist", selectedSong.getArtist());  // Pass song artist
            intent.putExtra("songAudioResId", songAudioResId);      // Pass the audio resource ID
            intent.putExtra("songLyricsResId", songLyricsResId);    // Pass the lyrics resource ID
            intent.putExtra("songVocalId", songVocalId);    // Pass the vocal resource ID
            startActivity(intent);
        });
    }

    // The method that will be called when the settings button is clicked
    public void openSettings(View view) {
        // Show a Toast message for debugging
        Toast.makeText(this, "Opening settings", Toast.LENGTH_SHORT).show();

        // Launch SettingsActivity when the settings button is clicked
        Intent intent = new Intent(SongListActivity.this, SettingsActivity.class);
        startActivity(intent);
    }
}
