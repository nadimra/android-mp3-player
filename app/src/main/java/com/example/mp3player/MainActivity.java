package com.example.mp3player;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;

/**
 * Main activity which allows you to play different songs from a list as well as controlling them.
 *
 * @author  Nadim Rahman
 * @version 1.0
 * @since   2020-11-17
 */
public class MainActivity extends AppCompatActivity {

    // GUI components
    private SeekBar mSeekBar;
    private Button musicControlButton;
    private LinearLayout playView;
    private TextView musicTitle;
    private Button stopButton;

    // Define connection and communication between service
    private MusicService.MyBinder myService = null;
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("g53mdp", "MainActivity onServiceConnected");
            myService = (MusicService.MyBinder) service;
            myService.registerCallback(callback);
            //MainActivity.this.runOnUiThread(checker);

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("g53mdp", "MainActivity onServiceDisconnected");
            myService.unregisterCallback(callback);
            myService = null;
        }
    };

    // Callback methods allow you to recieve information from the service
    ICallback callback = new ICallback() {
        // Update the seekbar whilst service is playing
        @Override
        public void counterEvent(final int counter, final int duration) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSeekBar.setProgress(counter);
                    mSeekBar.setMax(duration);
                }
            });
        }
        // Stop the music once service says it's ready
        @Override
        public void endMusic() {
            stopMusicScene();
        }
    };

    /**
     * Initialises the variables and displays the list of files
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise the variables
        mSeekBar = (SeekBar) findViewById(R.id.musicBar);
        mSeekBar.setEnabled(false);
        musicControlButton = findViewById(R.id.musicControl);
        stopButton = findViewById(R.id.stopButton);
        playView = findViewById(R.id.playView);
        playView.setVisibility(View.GONE);
        musicTitle = findViewById(R.id.musicTitle);

        // List all the files
        final ListView lv = (ListView) findViewById(R.id.listView);
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                null,
                null);
        lv.setAdapter(new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                cursor,
                new String[] { MediaStore.Audio.Media.DATA},
                new int[] { android.R.id.text1 }));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter,
                                    View myView,
                                    int myItemInt,
                                    long mylng) {
                Cursor c = (Cursor) lv.getItemAtPosition(myItemInt);
                String uri = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));

                // User clicks a song
                setMusicScene(uri);
            }
        });

    }



    /**
     * Handled when the user chooses a new song.
     * @param uri is the directory of the mp3 file
     */
    private void setMusicScene(String uri){
        // Checks if a connection already exists, if so unbind to reset the service
        if(myService!=null){
            unbindService(serviceConnection);
            stopService(new Intent(MainActivity.this, MusicService.class));
        }
        // Create a new service and bind it
        Intent serviceIntent = new Intent(MainActivity.this, MusicService.class);
        serviceIntent.putExtra("song",uri);
        startService(serviceIntent);
        bindService(new Intent(MainActivity.this, MusicService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        setMusicSceneGui(uri);
    }

    /**
     * Update the gui when the user chooses a song
     * @param uri is the directory of the mp3 file
     */
    private void setMusicSceneGui(String uri){
        musicControlButton.setText("PAUSE");
        playView.setVisibility(View.VISIBLE);
        File f = new File(uri);
        musicTitle.setText("Now Playing: " + f.getName());
    }

    /**
     * Handled the song has finished or manually stopped by the user.
     */
    public void stopMusicScene(){
        // Unbind the service and stop the service
        if(myService!= null) {
            unbindService(serviceConnection);
            stopService(new Intent(MainActivity.this, MusicService.class));
            myService = null;
        }
        // Remove the play view from the screen
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        Log.d("g53mdp", "MainActivity onStart");


        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d("g53mdp", "MainActivity onStop");
        super.onStop();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        Log.d("g53mdp", "MainActivity onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d("g53mdp", "MainActivity onPause");
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        Log.d("g53mdp", "MainActivity onDestroy");

        // Unbind from service to allow service to keep running in background
        if(myService != null) {
            unbindService(serviceConnection);
            myService = null;
        }
        super.onDestroy();
    }

    /**
     * Controls the music when the user clicks on play/pause
     */
    public void onMusicControlButtonClick(View v){
        if(myService.getState() == MP3Player.MP3PlayerState.PLAYING){
            myService.pauseMusic();
            musicControlButton.setText("PLAY");
        }else if(myService.getState() == MP3Player.MP3PlayerState.PAUSED){
            myService.playMusic();
            musicControlButton.setText("PAUSE");
        }
    }

    /**
     * Handled when user clicks the stop button
     */
    public void onStopButtonClick(View v){
        stopMusicScene();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if(myService==null){
            // myService is null whenever no music is being played
            outState.putBoolean("playingMusic",false);
        }else{
            // Store the music directory so that you can retrieve information on restore
            outState.putBoolean("playingMusic",true);
            outState.putString("musicLink",myService.getFilePath());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Boolean wasPlayingMusic = savedInstanceState.getBoolean("playingMusic");

        // Check if activity was playing music before it was destroyed
        if(wasPlayingMusic){
            String uri = savedInstanceState.getString("musicLink");
            // Bind the service again
            bindService(new Intent(MainActivity.this, MusicService.class), serviceConnection, Context.BIND_AUTO_CREATE);
            setMusicSceneGui(uri);
        }

    }

}