package com.example.karaoke;

public class Song {
    private String title;
    private String artist;
    private int imageResource;
    private int audioResourceId;  // Store the audio resource ID
    private int lyricsResourceId; // Store the lyrics resource ID
    private int vocalResourceId;

    // Constructor to initialize the Song object with audio and lyrics resource IDs
    public Song(String title, String artist, int imageResource, int audioResourceId, int lyricsResourceId, int vocalResourceId) {
        this.title = title;
        this.artist = artist;
        this.imageResource = imageResource;
        this.audioResourceId = audioResourceId;
        this.lyricsResourceId = lyricsResourceId;  // Initialize lyrics resource ID
        this.vocalResourceId = vocalResourceId;
    }

    public String getTitle() {
        return title;
    }
    public String getArtist(){return artist;}

    public int getImageResource() {
        return imageResource;
    }

    public int getAudioResourceId() {
        return audioResourceId;  // Return the audio resource ID
    }

    public int getLyricsResourceId() {
        return lyricsResourceId;  // Return the lyrics resource ID
    }
    public int getVocalResourceId(){
        return vocalResourceId;
    }
}
