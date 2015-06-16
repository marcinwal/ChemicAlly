package eu.ratingpedia.chemically;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;



public class MainActivity extends Activity {

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaPlayer = MediaPlayer.create(this,R.raw.music1);
        mediaPlayer.start();
    }


}
