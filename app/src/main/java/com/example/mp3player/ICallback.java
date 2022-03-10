package com.example.mp3player;

/**
 * Acts as communication to the service
 *
 * @author  Nadim Rahman
 * @version 1.0
 * @since   2020-11-19
 */
public interface ICallback {
    public void counterEvent(int counter,int duration);
    public void endMusic();

}
