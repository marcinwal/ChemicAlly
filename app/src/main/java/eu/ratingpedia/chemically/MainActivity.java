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
import java.util.Arrays;
import java.util.Random;


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
    int [] title;




    Intent myIntent;//starting hte game with touch screen



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configDisplay();
        loadElements();
        scaleElements();
        title = new int[]{6,2,0,20,53,53,39};
        mediaPlayer = MediaPlayer.create(this,R.raw.music1);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();


        elementsAnimate = new ElementsAnimate(this);
        setContentView(elementsAnimate);


        myIntent = new Intent(this,GameActivity.class);

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

        numBlocksWidth = 11;
        blockSize = screenWidth / numBlocksWidth;
        numBlocksHeight = screenHeight / blockSize;

    }

    private void loadElements() {
       for(int i=0; i < elements.length;i++){
            String name;
            name = "elem"+Integer.toString(i);
            int id = getResources().getIdentifier(name, "drawable",getPackageName());
           elements[i] = BitmapFactory.decodeResource(getResources(), id);
       }
    }

    private void scaleElements(){
        for(int i = 0;i < elements.length;i++){
            if (i == 0 || i == 2 || i == 6 || i == 20 || i == 39 || i == 53) {
                elements[i] = Bitmap.createScaledBitmap(elements[i], blockSize, blockSize, false);
            }else{
                elements[i] = Bitmap.createScaledBitmap(elements[i], blockSize/2, blockSize/2, false);
                Log.i("scaleSmaller:",""+i);
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
                elementsBackgroud();
                paint.setColor(Color.argb(255, 184, 138, 0));
                paint.setTextSize(150);
                canvas.drawText("ChemicAlly", 50, 150, paint);
                int offset = 2;
                for (int i = 0; i < title.length;i++){
                    canvas.drawBitmap(elements[title[i]],(offset+i)*blockSize,screenHeight/2-blockSize/2,paint);
                }

                ourHolder.unlockCanvasAndPost(canvas);
            }
            
        }

        private void elementsBackgroud() {
            Random randInt = new Random();
            int testX,testY;
            for(int i = 0; i < 9;i++ ) {
                testX = randInt.nextInt(numBlocksWidth);
                testY = randInt.nextInt(numBlocksWidth);
                canvas.drawBitmap(elements[randInt.nextInt(elements.length-1)], testX * blockSize, testY * blockSize, paint);
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
            try {
                startActivity(myIntent);
                mediaPlayer.release();
            }catch(Exception e){
                Log.e("","error:"+e.toString());
            }
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
