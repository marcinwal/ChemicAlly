package eu.ratingpedia.chemically;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.text.DecimalFormat;


public class MainActivity extends Activity {

    MediaPlayer mediaPlayer;
    int numberOfElements;

    ElementsAnimate elementsAnimate;

    Canvas canvas;
    Bitmap [] elements;
    int frameHeight;
    int frameWidth;

    int screenWidth;
    int screenHeight;

    long lastFrameTime;
    int fps;
    int blockSize;
    int numBlocksWidth;
    int numBlocksHeight;




    Intent i;//starting hte game with touch screen



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configDisplay();
        loadElements();
        scaleElements();

        mediaPlayer = MediaPlayer.create(this,R.raw.music1);
        mediaPlayer.start();


        elementsAnimate = new ElementsAnimate(this);
        setContentView(elementsAnimate);


        i = new Intent(this,GameActivity.class);

    }

    private void configDisplay(){
        Display display = getWindowManager().getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);

        numberOfElements = 60;
        elements = new Bitmap[numberOfElements];

        screenWidth = size.x;
        screenHeight = size.y;
        Log.i("sizex",""+screenWidth);
        Log.i("sizey",""+screenHeight);

        numBlocksWidth = 15;
        blockSize = screenWidth / numBlocksWidth;
        numBlocksHeight = screenHeight / blockSize;

    }

    private void loadElements() {
       for(int i=1; i < numberOfElements+1;i++){
            String name;
            name = "elem"+Integer.toString(i);
            int id = getResources().getIdentifier(name, "drawable",getPackageName());
            elements[i-1] = BitmapFactory.decodeResource(getResources(), id);
        }
    }

    private void scaleElements(){
        for(int i = 0;i < numberOfElements;i++){
            try {
                elements[i] = Bitmap.createScaledBitmap(elements[i], blockSize, blockSize, false);
                Log.i("info", "" + i);
            }catch(Exception e) {
                Log.e("", "error: " + e.toString());
            }
        }
    }




    private class ElementsAnimate extends SurfaceView implements Runnable {

        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingElements;
        Paint paint;

        public ElementsAnimate(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();
            frameWidth = elements[0].getWidth();
            frameHeight = elements[0].getHeight();

        }

        @Override
        public void run() {
            while(playingElements){
                drawElements();
                controlFPS();
            }
        }

        private void placeRandomly() {
        }

        private void drawElements() {

            //paint.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.BLACK, Color.WHITE, Shader.TileMode.MIRROR));

            if(ourHolder.getSurface().isValid()){
                canvas = ourHolder.lockCanvas();

                canvas.drawColor(Color.DKGRAY);
                paint.setColor(Color.argb(255, 184, 138, 0));
                paint.setTextSize(150);
                canvas.drawText("ChemicAlly", 50, 150, paint);
                canvas.drawBitmap(elements[0],150,150,paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }
            
        }

        private void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis()-lastFrameTime);
            long timeToSleep = 500 - timeThisFrame;
            if(timeThisFrame > 0){
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0){
                try{
                    ourThread.sleep(timeToSleep);
                }catch (InterruptedException e){

                }
            }
            lastFrameTime = System.currentTimeMillis();
        }

        public void pause(){
            playingElements = false;
            try{
                mediaPlayer.release();
                ourThread.join();
            }catch (InterruptedException e){

            }
        }

        public void resume(){
            playingElements = true;
            ourThread = new Thread(this);
            ourThread.start();
            mediaPlayer.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent){
            startActivity(i);
            mediaPlayer.release();
            return true;
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        while(true){
            elementsAnimate.pause();
            break;
        }
        finish();
    }

    @Override
    protected void onResume(){
        super.onResume();
        elementsAnimate.resume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        elementsAnimate.pause();
    }

    public boolean onKeyDown(int keyCode,KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            elementsAnimate.pause();
            finish();
            return true;
        }
        return false;
    }
}
