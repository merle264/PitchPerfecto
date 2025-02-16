package com.example.karaoke;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class SongAdapter extends BaseAdapter {

    private Context context;
    private List<Song> songs;
    private LayoutInflater inflater;

    public SongAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.song_list_item, parent, false);
        }

        ImageView songImageView = convertView.findViewById(R.id.songImageView);
        TextView songTitle = convertView.findViewById(R.id.songTitle);
        TextView songArtist = convertView.findViewById(R.id.songArtist);

        Song song = songs.get(position);

        // Set the song title
        songTitle.setText(song.getTitle());

        songArtist.setText(song.getArtist());

        // Set the image for all songs to the same image (ic_music_note)
        songImageView.setImageResource(R.drawable.ic_music_note);

        return convertView;
    }
}
