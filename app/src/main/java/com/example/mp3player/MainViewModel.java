package com.example.mp3player;
import android.graphics.Color;

import androidx.lifecycle.MutableLiveData;

import com.example.mp3player.ObservableViewModel;

/**
 * This class is the view model for MainActivity
 *
 * @author  Nadim Rahman
 * @version 1.0
 * @since   2020-10-29
 */
public class MainViewModel extends ObservableViewModel {
    private MutableLiveData<String> currentSong;
    private MutableLiveData<MP3Player.MP3PlayerState> currentState;
    private MutableLiveData<Integer> currentDuration;


    public MutableLiveData<String> getCurrentSong() {
        if (currentSong == null) {
            currentSong = new MutableLiveData<String>();
            currentSong.setValue("Test");
        }
        return currentSong;
    }

    public void setCurrentSong(String song){
        getCurrentSong().setValue(song);
    }

    public MutableLiveData<MP3Player.MP3PlayerState> getCurrentState() {
        if (currentState == null) {
            currentState = new MutableLiveData<MP3Player.MP3PlayerState>();
            currentState.setValue(MP3Player.MP3PlayerState.STOPPED);
        }
        return currentState;
    }

    public void setCurrentState(MP3Player.MP3PlayerState state){
        getCurrentState().setValue(state);
    }
}
