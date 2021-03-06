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

import java.util.Random;


public class MainActivity extends Activity {

    MediaPlayer mediaPlayer;
    int numberOfElements;

    ElementsAnimate elementsAnimate;

    Canvas canvas;
    public static Bitmap [] elements;
    int frameHeight;
    int frameWidth;

    public static int screenWidth;
    public static int screenHeight;

    long lastFrameTime;
    int fps;
    int blockSize;
    int numBlocksWidth;
    int numBlocksHeight;
    int [] title;
    int titleScale = 150; //100 normal
    public static int [] selectedElements = new int[]{0,2,6,20,39,53,1,7,8,11,12,15,16,17,19,20,127,128,129,130};
    public static String [] namesElements = new String[]{"Mi","He","C","Ca","Y","I","H","N","O",
    //                                                    a  , b  , c , d  , e , f , g , h , i ,
                                                        "Na","Mg","P","S","Cl","K","Ca","Wall","Reset","-","+"};
    //                                                    j , k  , l , m , n  , o , p  , q    ,   r   , s , t




    static Intent myIntent;//NOWADDED static



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configDisplay();

        loadSelectedAndScale();

        title = new int[]{2,1,0,3,5,5,4};
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

        numberOfElements = 20;
        elements = new Bitmap[numberOfElements];

        screenWidth = size.x;
        screenHeight = size.y;


        numBlocksWidth = 11;
        blockSize = screenWidth / numBlocksWidth;
        numBlocksHeight = screenHeight / blockSize;

    }

    public void loadElements() {
       for(int i=0; i < elements.length;i++){
            String name;
            name = "elem"+Integer.toString(i);
            int id = getResources().getIdentifier(name, "drawable",getPackageName());
            elements[i] = BitmapFactory.decodeResource(getResources(), id);

       }
    }

    public void loadSelectedAndScale(){
        for(int i=0; i < selectedElements.length;i++){
            String name;
            name = "elem" + Integer.toString(selectedElements[i]);
            int id = getResources().getIdentifier(name, "drawable",getPackageName());
            elements[i] = BitmapFactory.decodeResource(getResources(), id);
            if (i < 6){
                elements[i] = Bitmap.createScaledBitmap(elements[i],(blockSize*titleScale)/100, (blockSize*titleScale)/100, false);
            }else{
                elements[i] = Bitmap.createScaledBitmap(elements[i], blockSize/2, blockSize/2, false);
            }

        }
    }

    private void scaleElements(){
        for(int i = 0;i < elements.length;i++){
            if (i == 0 || i == 2 || i == 6 || i == 20 || i == 39 || i == 53) { //to Change but not looping
                elements[i] = Bitmap.createScaledBitmap(elements[i],(blockSize*titleScale)/100, (blockSize*titleScale)/100, false);
            }else{
                elements[i] = Bitmap.createScaledBitmap(elements[i], blockSize/2, blockSize/2, false);
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



        private void drawElements() {

            //paint.setShader(new LinearGradient(0, 0, 0, getHeight(), Color.BLACK, Color.WHITE, Shader.TileMode.MIRROR));

            if(ourHolder.getSurface().isValid()){
                canvas = ourHolder.lockCanvas();

                canvas.drawColor(Color.DKGRAY);
                elementsTestGrid(selectedElements.length-10,6);


                paint.setColor(Color.argb(255, 184, 138, 0));
                paint.setTextSize(150);
                int offset = (screenWidth - 7 * elements[0].getWidth()) / 2;
                for (int i = 0; i < title.length;i++){
                    canvas.drawBitmap(elements[title[i]],offset+i*(blockSize*titleScale)/100,screenHeight/2-(blockSize*titleScale)/100/2,paint);
                }

                ourHolder.unlockCanvasAndPost(canvas);
            }
            
        }


        private void elementsTestGrid(int howMany,int offset){
            paint.setAlpha(100);
            Random randInt = new Random();
            for(int i = 0;i < 22;i++)
                for(int j = 0; j < 14;j++){
                    int elem = randInt.nextInt(howMany-1)+offset;
                    canvas.drawBitmap(elements[elem], i * blockSize / 2, j * blockSize / 2, paint);
                }
            paint.setAlpha(255);
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
                    Log.e("error",e.getMessage());
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
                Log.e("error",e.getMessage());
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
                //elements = null; //i am not sure if it solves the problem of memory usage
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
