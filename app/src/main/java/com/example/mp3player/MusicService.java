package com.example.mp3player;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;

/**
 * This class is a service which plays the audio from a track
 *
 * @author  Nadim Rahman
 * @version 1.0
 * @since   2020-11-17
 */
public class MusicService extends Service{

    private static int numClients;
    private final IBinder binder = new MyBinder();
    private MP3Player player;
    private final String CHANNEL_ID = "100";
    int NOTIFICATION_ID = 001;
    private NotificationManager notificationManager;
    RemoteCallbackList<MyBinder> remoteCallbackList = new RemoteCallbackList<MyBinder>();
    protected Counter counter;

    /**
     * Continually checks the progress of the current song
     */
    protected class Counter extends Thread implements Runnable {
        public boolean running = true;
        public boolean endMusic = false;
        public Counter() {
            this.start();
        }

        public void run() {
            try {Thread.sleep(1000);} catch(Exception e) {return;}

            while(this.running) {
                // Return progress to the main activity
                doCallbacks(player.getProgress()/1000,player.getDuration()/1000);

                // Check if the song has finished
                if(player.getProgress() >= player.getDuration()){
                    // Check if there are activities bound
                    if(MusicService.numClients>=1){
                        doCallbackEndMusic();
                        stopSelf();
                    }else{
                        stopSelf();
                    }

                }


            }
            Log.d("g53mdp", "Service counter thread exiting");
        }
    }

    /**
     * Communicate with all the bounded activities and send progress
     * @param count song press so far
     * @param duration song duration
     */
    public void doCallbacks(int count,int duration) {
        final int n = remoteCallbackList.beginBroadcast();
        for (int i=0; i<n; i++) {
            remoteCallbackList.getBroadcastItem(i).callback.counterEvent(count,duration);
        }
        remoteCallbackList.finishBroadcast();
    }
    /**
     * Communicate with all the bounded activities and tell it to end the music and unbind
     */
    public void doCallbackEndMusic() {
        final int n = remoteCallbackList.beginBroadcast();
        for (int i=0; i<n; i++) {
            remoteCallbackList.getBroadcastItem(i).callback.endMusic();
        }
        remoteCallbackList.finishBroadcast();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        numClients++;
        return binder;
    }

    /**
     * Handles creation of service and creates a new player and counter.
     */
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        Log.d("g53mdp", "MusicService Service onCreate");
        super.onCreate();
        player = new MP3Player();
        counter = new Counter();
    }

    /**
     * Handles destruction of service. Resets the variables
     */
    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.d("g53mdp", "MusicService service destroyed");
        counter.running = false;
        counter = null;
        player.stop();
        player = null;
        notificationManager.cancelAll();
        super.onDestroy();
    }

    /**
     * Reloads a new song and creates the notication for it
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String song = intent.getStringExtra("song");
        if(player.getState()== MP3Player.MP3PlayerState.STOPPED){
            player.load(song);
        }else{
            player.stop();
            player.load(song);

        }

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel name";
            String description = "channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name,importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }

        File f = new File(song);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        NotificationManager nMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        PendingIntent intent2 = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this,
                CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("MP3 Player")
                .setContentText(f.getName() + " is currently playing")
                .setContentIntent(intent2)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        startForeground(NOTIFICATION_ID,mBuilder.build());
        return START_STICKY;
    }

    /**
     * Class supports communication with the main activity
     */
    public class MyBinder extends Binder implements IInterface {

        void playMusic(){
            player.play();
        }

        MP3Player.MP3PlayerState getState(){
            return player.getState();
        }

        int getDuration(){
            return player.getDuration();
        }

        void stopMusic(){
            player.stop();
        }

        void pauseMusic(){
            player.pause();
        }

        int getProgress(){
            return player.getProgress();
        }

        void changeSong(String uri){
            Log.d("g53mdp", "MusicService Service changing song");

            player.stop();
            player.load(uri);
            Log.d("g53mdp", "MusicService Service changing done");

        }

        MP3Player getPlayer(){
            return player;
        }
        String getFilePath(){
            return player.getFilePath();
        }

        public void registerCallback(ICallback callback) {
            this.callback = callback;
            remoteCallbackList.register(MyBinder.this);
        }

        public void unregisterCallback(ICallback callback) {
            remoteCallbackList.unregister(MyBinder.this);
        }

        ICallback callback;

        @Override
        public IBinder asBinder() {
            return this;
        }
    }

    @Override
    public void onRebind(Intent intent) {
        // TODO Auto-generated method stub
        Log.d("g53mdp", "service onRebind");
        numClients++;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        Log.d("g53mdp", "service onUnbinddd");
        numClients--;
        return super.onUnbind(intent);
    }
}
