package eu.ratingpedia.chemically;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.os.Bundle;



public class MainActivity extends Activity {

    MediaPlayer mediaPlayer;

    Canvas canvas;
    Bitmap [] elements;
    int frameHeight;
    int frameWidth;

    int screenWidth;
    int getScreenHeight;

    long lastFrameTime;
    int fps;
    int hi;

    Intent i;//strating hte game with touch screen


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = MediaPlayer.create(this,R.raw.music1);
        mediaPlayer.start();
    }


}
