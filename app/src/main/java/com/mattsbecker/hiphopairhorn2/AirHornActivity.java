package com.mattsbecker.hiphopairhorn2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class AirHornActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final int SONG_SELECT_RESULT = 100;
    private static final int LOAD_AD_MODE_NEW = 0;
    private static final int LOAD_AD_MODE_RESUME = 1;

    // keep track of the number of times a user selects a song.
    int songSelectCount = 0;

    private String _track;
    private LinearLayout adViewContainer;
    private RelativeLayout auxControlsLayout;
    private ImageButton btn;
    private Context c;
    private RelativeLayout contentViewContainer;
    private Handler handler = new Handler();
    private boolean hasExternalStorage = false;
    private SoundPool hornPlayer;
    private int hornSoundId;
    private RelativeLayout mainLayout;
    private ImageButton pauseButton;
    private ImageButton playButton;
    private Timer seekTimer;
    private View.OnClickListener selectSong = new View.OnClickListener()
    {
        public void onClick(View paramAnonymousView)
        {
            AirHornActivity.this.selectingSong = true;
            //if (AirHornActivity.this.hasExternalStorage)
            //{
            Intent localIntent = new Intent(AirHornActivity.this.c, SongSelectActivity.class);
            AirHornActivity.this.startActivityForResult(localIntent, SONG_SELECT_RESULT);
            //}
        }
    };
    private RelativeLayout selectSongButton;
    private String selectedArtist;
    private boolean selectingSong = false;
    private boolean didLoadInterstitial = false;
    private MediaPlayer songPlayer;
    private String track;
    private TimerTask updateSeekTask;
    private boolean playServicesAvailable = true;
    private LinearLayout adContainer;
    private AdView admobAdView;
    private InterstitialAd admobInterstitial;


    private void beginSeekBarUpdate()
    {
        this.updateSeekTask = new TimerTask()
        {
            public void run()
            {
                AirHornActivity.this.handler.post(new Runnable()
                {
                    public void run()
                    {
                        if ((AirHornActivity.this.songPlayer != null) && (AirHornActivity.this.songPlayer.getDuration() != 0))
                        {
                            int i = AirHornActivity.this.songPlayer.getDuration();
                            int j = AirHornActivity.this.songPlayer.getCurrentPosition();
                            AirHornActivity.this.updateSeekBarWithDurationAndPosition(i, j);
                            return;
                        }
                        AirHornActivity.this.updateSeekBarWithDurationAndPosition(0, 0);
                    }
                });
            }
        };
        this.seekTimer.scheduleAtFixedRate(this.updateSeekTask, 1000L, 1000L);
    }

    private void loadAdvertisement(int mode)
    {
        // load an ad so I can make some ca$h.
        if (this.playServicesAvailable) {
            if (mode == LOAD_AD_MODE_NEW) {
                AdRequest adRequest = new AdRequest.Builder()
                        .addTestDevice("88F69322C5A87465824017D41D554E0D")
                        .build();
                admobAdView.loadAd(adRequest);
            } else if (mode == LOAD_AD_MODE_RESUME) {
                admobAdView.resume();
            }
        }
    }

    private void showInterstitial(final int paramInt1, final int paramInt2, final Intent paramIntent) {
        // Show the ad if it's ready. Otherwise toast and restart the game.
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        admobInterstitial.loadAd(adRequest);
        admobInterstitial.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                //beginPlayingTrack();
                super.onAdClosed();
                handleTrackLoading(paramInt1, paramInt2, paramIntent);
            }
            @Override
            public void onAdFailedToLoad(int errorCode) {
                super.onAdFailedToLoad(errorCode);
            }

            @Override
            public void onAdLeftApplication() {
                super.onAdLeftApplication();
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                if (didLoadInterstitial) {
                    songPlayer.pause();
                };
            }
        });

        if (admobInterstitial != null && admobInterstitial.isLoaded()) {
            didLoadInterstitial = true;
            admobInterstitial.show();
        } else {
            didLoadInterstitial = false;
            this.beginPlayingTrack();
        }
    }

    private void playSound()
    {
        this.hornPlayer.play(this.hornSoundId, 1.0F, 1.0F, 1, 0, 1.0F);
    }

    private void setCurrentlyPlayingText(String paramString1, String paramString2)
    {
        String str = "";
        if ((paramString1 == null) && (paramString2 == null))
        {
            ((TextView)findViewById(R.id.song_title_text)).setText("Add some swag to...");
            return;
        }
        if (paramString1 != null)
            str = paramString1;
        if (paramString2 != null)
            str = str + " - " + paramString2;
        if ((str != null) && (str.length() > 0))
        {
            ((TextView)findViewById(R.id.song_title_text)).setText(str);
            return;
        }
        ((TextView)findViewById(R.id.song_title_text)).setText("Add some swag to...");
    }

    private void updateSeekBarWithDurationAndPosition(int paramInt1, int paramInt2)
    {
        ((SeekBar)findViewById(R.id.seekBar)).setMax(paramInt1);
        ((SeekBar)findViewById(R.id.seekBar)).setProgress(paramInt2);
    }

    public void finish()
    {
        if (this.songPlayer != null)
        {
            this.songPlayer.pause();
            this.songPlayer.stop();
            this.songPlayer.reset();
        }
        super.finish();
    }

    private void beginPlayingTrack() {
        this.songPlayer.start();
        beginSeekBarUpdate();
        setCurrentlyPlayingText(this.track, this.selectedArtist);
    }

    private void handleTrackLoading(int paramInt1, int paramInt2, Intent paramIntent) {
        songSelectCount++;
        this.selectingSong = false;
        if ((paramInt2 == -1) && (paramInt1 == SONG_SELECT_RESULT) && (paramIntent.getStringExtra("track").length() != 0) && (paramIntent.getStringExtra("artist").length() != 0))
        {
            this.track = paramIntent.getStringExtra("track");
            this.selectedArtist = paramIntent.getStringExtra("artist");
            if (this.songPlayer == null)
                this.songPlayer = new MediaPlayer();
            Uri localUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] arrayOfString = { "title", "_data" };
            String str = "title".toString() + " = " + DatabaseUtils.sqlEscapeString(this.track);
            Cursor localCursor = getContentResolver().query(localUri, arrayOfString, str, null, null);
            localCursor.moveToNext();
            int i = localCursor.getColumnIndexOrThrow("_data");
            this.track = localCursor.getString(localCursor.getColumnIndexOrThrow("title"));
            try
            {
                updateSeekBarWithDurationAndPosition(0, 0);
                this.songPlayer.pause();
                this.songPlayer.stop();
                this.songPlayer.reset();
                this.songPlayer = null;
                this.songPlayer = new MediaPlayer();
                this._track = localCursor.getString(localCursor.getColumnIndexOrThrow("title"));
                this.songPlayer.setDataSource(getApplicationContext(), Uri.parse(localCursor.getString(i)));
                this.songPlayer.prepare();
                beginPlayingTrack();
                // if even, show an interstitial. Hehe.
                if (songSelectCount % 3 == 0) {
                    showInterstitial(paramInt1, paramInt2, paramIntent);
                    return;
                    // TODO: Ensure we begin playing the track after the interstitial is closed
                }
                return;
            }
            catch (IllegalArgumentException localIllegalArgumentException)
            {
                localIllegalArgumentException.printStackTrace();
                return;
            }
            catch (SecurityException localSecurityException)
            {
                localSecurityException.printStackTrace();
                return;
            }
            catch (IllegalStateException localIllegalStateException)
            {
                localIllegalStateException.printStackTrace();
                return;
            }
            catch (IOException localIOException)
            {
                localIOException.printStackTrace();
            }
        }
    }

    protected void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent)
    {
        this.handleTrackLoading(paramInt1, paramInt2, paramIntent);
    }

    public void onConfigurationChanged(Configuration paramConfiguration)
    {
        super.onConfigurationChanged(paramConfiguration);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        // set our content view
        setContentView(R.layout.activity_layout_airhorn);

        // configure admod

        playServicesAvailable = configureGooglePlayServices();

        Environment.getExternalStorageState();
        if ((Environment.getExternalStorageState() == "removed") || (Environment.getExternalStorageState() == "mounted"));
        // make sure this device can actually store media
        for (this.hasExternalStorage = false; ; this.hasExternalStorage = true)
        {
            this.c = this;
            this.hornPlayer = new SoundPool(1, 3, 0);
            this.hornSoundId = this.hornPlayer.load(getApplicationContext(), R.raw.air_horn, 1);
            this.btn = ((ImageButton)findViewById(R.id.swag_button));
            this.btn.setOnTouchListener(new View.OnTouchListener()
            {
                public boolean onTouch(View paramAnonymousView, MotionEvent paramAnonymousMotionEvent)
                {
                    if (paramAnonymousMotionEvent.getAction() == 0)
                    {
                        AirHornActivity.this.playSound();
                        AirHornActivity.this.btn.setBackgroundResource(R.drawable.button_pressed);
                    }
                    while (true)
                    {
                        if (paramAnonymousMotionEvent.getAction() == 1) {
                            AirHornActivity.this.btn.setBackgroundResource(R.drawable.button);
                        }
                        return false;
                    }
                }
            });
            this.playButton = ((ImageButton)findViewById(R.id.play_button));
            this.playButton.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View paramAnonymousView, MotionEvent paramAnonymousMotionEvent) {
                    if ((AirHornActivity.this.songPlayer != null) && (!AirHornActivity.this.songPlayer.isPlaying()))
                        AirHornActivity.this.songPlayer.start();
                    return false;
                }
            });
            this.pauseButton = ((ImageButton)findViewById(R.id.pause_button));
            this.pauseButton.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View paramAnonymousView, MotionEvent paramAnonymousMotionEvent) {
                    if ((AirHornActivity.this.songPlayer != null) && (AirHornActivity.this.songPlayer.isPlaying()))
                        AirHornActivity.this.songPlayer.pause();
                    return false;
                }
            });
            ((SeekBar)findViewById(R.id.seekBar)).setProgress(0);
            this.selectSongButton = ((RelativeLayout)findViewById(R.id.track_name_container));
            this.selectSongButton.setOnClickListener(this.selectSong);
            this.adContainer = (LinearLayout)findViewById(R.id.ad_container);
            this.admobAdView = (AdView)findViewById(R.id.admobAdView);
            // request a new ad!
            this.loadAdvertisement(LOAD_AD_MODE_NEW);

            // Create the InterstitialAd and set the adUnitId.
            admobInterstitial = new InterstitialAd(this);
            admobInterstitial.setAdUnitId(getString(R.string.admob_interstitial));
            return;
        }
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
        if ((!this.selectingSong) && (this.songPlayer != null))
        {
            this.songPlayer.pause();
            this.songPlayer.stop();
            this.songPlayer.reset();
            //this.songPlayer = null;
            this.seekTimer.cancel();
        }
        admobAdView.pause();
        super.onPause();
    }


    public void onResume()
    {
        super.onResume();
        if ((this.songPlayer != null) && (!this.songPlayer.isPlaying()))
            setCurrentlyPlayingText(null, null);
        this.seekTimer = new Timer();
        loadAdvertisement(LOAD_AD_MODE_RESUME);
    }

    protected void onUserLeaveHint()
    {
        if (!this.selectingSong)
        {
            if (this.songPlayer != null)
            {
                this.songPlayer.pause();
                this.songPlayer.stop();
                this.songPlayer.reset();
            }
            super.onUserLeaveHint();
        }
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
