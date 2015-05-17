package com.mattsbecker.hiphopairhorn2;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

/**
 * Created by matt on 5/9/15.
 */
public class SongSelectCusorAdapter extends CursorAdapter {
    public SongSelectCusorAdapter(Context context, Cursor cursor) {
        super(context, cursor,0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.layout_songselect_trackinfo, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView trackNameTextView = (TextView) view.findViewById(R.id.track_name);
        TextView trackArtistTextView = (TextView) view.findViewById(R.id.track_artist);
        // Extract properties from cursor
        String trackTitle = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        String artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
        // Populate fields with extracted properties
        trackNameTextView.setText(trackTitle);
        trackArtistTextView.setText(String.valueOf(artist));

    }
}
