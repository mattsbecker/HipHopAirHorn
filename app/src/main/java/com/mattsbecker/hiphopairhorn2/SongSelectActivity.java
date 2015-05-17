package com.mattsbecker.hiphopairhorn2;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;

public class SongSelectActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private static final int LOAD_AD_MODE_NEW = 0;
    private static final int LOAD_AD_MODE_RESUME = 1;
    String artist = "";
    String track = "";
    ListView listView;
    private boolean playServicesAvailable = true;
    private AdView admobAdView;

    private void loadAdvertisement(int mode)
    {
        // load an ad so I can make some ca$h.
        if (this.playServicesAvailable) {
            if (mode == LOAD_AD_MODE_NEW) {
                AdRequest adRequest = new AdRequest.Builder().build();
                admobAdView.loadAd(adRequest);
            } else if (mode == LOAD_AD_MODE_RESUME) {
                admobAdView.resume();
            }
        }
    }
    private Cursor showSongList()
    {
        Uri localUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] arrayOfString = { "_id", "title", "artist", "album", "_data" };
        Cursor localCursor = getContentResolver().query(localUri, arrayOfString, null, null, null);
        if (localCursor != null) {
            localCursor.moveToNext();
        }
        return localCursor;
    }

    public void finish()
    {
        Intent localIntent = new Intent();
        Bundle localBundle = new Bundle();
        localBundle.putString("artist", this.artist);
        localBundle.putString("track", this.track);
        localIntent.putExtras(localBundle);
        setResult(-1, localIntent);
        super.finish();
    }

    public void onConfigurationChanged(Configuration paramConfiguration)
    {
        super.onConfigurationChanged(paramConfiguration);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public void onCreate(Bundle paramBundle)
    {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_layout_songselect);
        this.listView = (ListView) findViewById(R.id.song_list);
        String[] arrayOfString = { "title", "artist" };
        int[] arrayOfInt = { R.id.track_name, R.id.track_artist };
        Cursor localCursor = showSongList();
        if (localCursor != null) {
            SongSelectCusorAdapter songSelectCusorAdapter = new SongSelectCusorAdapter(this, localCursor);
            this.listView.setAdapter(songSelectCusorAdapter);
            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Cursor localCursor = (Cursor)parent.getItemAtPosition(position);
                    artist = localCursor.getString(2);
                    track = localCursor.getString(1);
                    finish();
                }
            });
        }
        this.admobAdView = (AdView)findViewById(R.id.admobAdView);
        // request a new ad!
        this.loadAdvertisement(LOAD_AD_MODE_NEW);

    }

    public void onDestroy()
    {
        if (this.admobAdView != null) {
            this.admobAdView.destroy();
        }
        super.onDestroy();
    }

    protected void onPause()
    {
        admobAdView.pause();
        super.onPause();
    }

    public void onResume()
    {
        super.onResume();
        loadAdvertisement(LOAD_AD_MODE_RESUME);
    }
    /**
     * Configure Google Play services for serving advertisements.
     * Returns boolean
     *
     * true if Play Services is accessible
     * false is Play Services could not be reached
     * **/
    private boolean configureGooglePlayServices() {
        boolean result = true;
        GoogleApiClient client = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        client.connect();
        return result;
    }

    // Google Play Services Callbacks
    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the next section.
        // do stuff
    }
}
