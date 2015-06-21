package eu.ratingpedia.chemically;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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


public class GameActivity extends Activity {

    MediaPlayer mediaPlayer;
    Canvas canvas;
    Bitmap [] elements = MainActivity.elements; //to avoid loading again
    int [] selectedElements = MainActivity.selectedElements;
    int [][] gameGrid;
    long lastFrameTime;

    int maxNumberAtomsInMolecule = 10;

    Molecule playersMolecule; //players molecule scattered all over
    Molecule targetMolecule;  //target molecules

    int screenWidth = MainActivity.screenWidth;
    int screenHeight = MainActivity.screenHeight;
    int topGap;
    int rightGap;
    int leftGap;

    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;
    int numBlocksWideBoard;
    int numBlocksHighBoard;

    int targetLineIndicator;
    int numberOfPhases;

    int score;
    int hiScore;

    int fps;
    Intent i;

    boolean won;

    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaPlayer = MediaPlayer.create(this,R.raw.music2);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        playersMolecule = new Molecule();
        targetMolecule = new Molecule();

        i = new Intent(this,MainActivity.class);

        configureDisplay();

        gameView = new GameView(this);

        setContentView(gameView);

    }

    private void setBoard(){
        for(int i = 0; i < numBlocksWideBoard;i++)
            for(int j = 0;j < numBlocksHighBoard;j++)
            {
                gameGrid[i][j] = 0;//TODO fill it with WALLS etc.
            }
        Log.i("endMethod","setBoard");
    }

    private void configureDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        topGap = 0;//TODO for the time being
        rightGap =0;//TODO for the time being the whole screen

        numBlocksWide = 25;
        blockSize = screenWidth / numBlocksWide;
        numBlocksHigh = ((screenHeight - topGap))/blockSize;
        numBlocksWideBoard = numBlocksWide;//TODO minus right gap
        numBlocksHighBoard = numBlocksHigh;
        gameGrid = new int[numBlocksWideBoard][numBlocksHighBoard];

        targetLineIndicator = 2; //where to place the molecule
        numberOfPhases = 3;


        //scaling bitmaps for the screen
        for(int i = 0;i < elements.length;i++){
            elements[i] = Bitmap.createScaledBitmap(elements[i],
                          blockSize - targetLineIndicator,blockSize-targetLineIndicator,false);
        }
        Log.i("endMethod","configureDisplay");

    }

    public class Atom {

        int atomIdx,posX,posY,direction,phase;

        public Atom(int atomIdx,int posX,int posY){
            this.atomIdx = atomIdx;
            this.posX = posX;
            this.posY = posY;
            this.direction = 0; // not moving 1,2,3,4
            this.phase = 0;     // in what phase of animations it is for smooth animation
        }

        public void changeXY(int deltaX,int deltaY){
            this.posX += deltaX;
            this.posY += deltaY;
        }
    }

    public class Molecule {
        Atom [] atoms;
        int numberOfAtoms;

        public Molecule(){
            Log.i("myConstructorStart","molecule");
            atoms = new Atom[maxNumberAtomsInMolecule];
            numberOfAtoms = 0;
            Log.i("myConstructorEnd","molecule");
        }

        //adding atom to the molecule
        public void addAtomToMolecule(Atom atom){
            if(numberOfAtoms < maxNumberAtomsInMolecule ){
                atoms[numberOfAtoms++] = atom;
            }
        }

        public void moveMoleculeAtom(int atomIdx,int deltaX,int deltaY){
            atoms[atomIdx].changeXY(deltaX,deltaY);
        }

        //compares to molecules if they are equal, atoms must be placed in the same relative setup
        public boolean sameMolecule(Molecule molecule){
            boolean same = false;
            //first simple check just 1st atom in the molecule must
            //have the same type and exact location
            if(this.atoms[0].posX == molecule.atoms[0].posX &&
                    this.atoms[0].posY == molecule.atoms[0].posY &&
                    this.atoms[0].atomIdx == molecule.atoms[0].atomIdx){
                same = true;
            }
            return same;
        }
    }


    private class GameView extends SurfaceView implements Runnable {

        Thread ourThread;
        SurfaceHolder ourHolder;
        volatile boolean movingMolecules;
        volatile boolean playingMolecules;
        Paint paint;


        public GameView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            getBoard();
            getMoleculesPlayer();
            getMoleculesTarget();
            Log.i("endMethod", "GameViewConstructor");
        }

        private void getMoleculesTarget() {
            //TODO load it from the file
            Atom atom = new Atom(1,10,10);
            targetMolecule.addAtomToMolecule(atom);
        }

        private void getMoleculesPlayer() {
            Atom atom = new Atom(1,0,0);
            Log.i("getMoleculesPlayer","atom added");
            playersMolecule.addAtomToMolecule(atom);
            Log.i("getMoleculesPlayerInfo", "atom added to the molecule");
            Random intRandom = new Random();
            int x = intRandom.nextInt(5);
            int y = intRandom.nextInt(5);
            playersMolecule.moveMoleculeAtom(0, x, y);
            Log.i("endMethod", "getMoleculesPlayer");
        }

        //loading the board and setting the grid
        private void getBoard() {
            setBoard();
        }

        @Override
        public void run() {
            while(playingMolecules){
                updateGame();
                drawGame();
                controlFPS();
            }
        }

        //TODO check if a molecule can move further;
        private void updateGame() {

            //need to check if move to the next is allowed
            for(int i = 0;i < playersMolecule.numberOfAtoms;i++){
                if (playersMolecule.atoms[i].direction != 0){
                    playersMolecule.atoms[i].phase++;
                    if (playersMolecule.atoms[i].phase >= numberOfPhases){
                        playersMolecule.atoms[i].phase = 0; //all phases
                        //change positions on the grid
                        switch(playersMolecule.atoms[i].direction){
                            case 1:
                                playersMolecule.moveMoleculeAtom(i,0,-1);//going up
                                break;
                            case 2:
                                playersMolecule.moveMoleculeAtom(i,1,0);//going right
                                break;
                            case 3:
                                playersMolecule.moveMoleculeAtom(i,0,1);//going down
                                break;
                            case 4:
                                playersMolecule.moveMoleculeAtom(i,-1,0);//going left
                                break;
                        }
                    }
                    break;//only one molecule can move at the time
                }
            }

            won = playersMolecule.sameMolecule(targetMolecule);


        }

        //drawing walls
        private void drawWalls(){

        }

        private void drawTarget(){

            for(int i = 0 ; i < targetMolecule.numberOfAtoms;i++){

                paint.setColor(Color.argb(255, 255, 255, 255));
                canvas.drawRect(leftGap+targetMolecule.atoms[i].posX * blockSize,
                        topGap + targetMolecule.atoms[i].posY * blockSize,
                        leftGap + targetMolecule.atoms[i].posX * blockSize + blockSize,
                        topGap + targetMolecule.atoms[i].posY * blockSize + blockSize, paint);

                paint.setColor(Color.argb(255, 0, 0, 0));
                canvas.drawRect(leftGap+targetMolecule.atoms[i].posX * blockSize+targetLineIndicator,
                        topGap + targetMolecule.atoms[i].posY * blockSize+targetLineIndicator,
                        leftGap + targetMolecule.atoms[i].posX * blockSize+blockSize-targetLineIndicator,
                        topGap + targetMolecule.atoms[i].posY * blockSize+blockSize-targetLineIndicator,paint);
            }
        }

        private void drawGame() {
            if(ourHolder.getSurface().isValid()){
                canvas = ourHolder.lockCanvas();
                canvas.drawColor(Color.DKGRAY);
                //text ...
                drawTarget();
                drawWalls();
                paint.setColor(Color.argb(255, 255, 255, 255));
                for(int i = 0; i < playersMolecule.numberOfAtoms;i++){
                    canvas.drawBitmap(elements[playersMolecule.atoms[i].atomIdx],
                            leftGap + playersMolecule.atoms[i].posX * blockSize+targetLineIndicator,
                            topGap + playersMolecule.atoms[i].posY * blockSize+targetLineIndicator,paint);
                }

                ourHolder.unlockCanvasAndPost(canvas);
            }

        }

        private void controlFPS() {
            long timeThisFrame = System.currentTimeMillis() - lastFrameTime;
            long timeToSleep = 100 - timeThisFrame;
            if(timeThisFrame > 0){
                fps = (int) (1000/timeThisFrame);
            }
            if(timeToSleep > 0){
                try{
                    ourThread.sleep(timeToSleep);
                }catch(InterruptedException e){
                    Log.e("Error:",e.toString());
                }
            }
            lastFrameTime = System.currentTimeMillis();
        }

        public void pause(){
            playingMolecules = false;
            try{
                mediaPlayer.release();
                ourThread.join();
            }catch(InterruptedException e){
                Log.e("Error:",e.toString());
            }
        }

        public void resume(){
            mediaPlayer.start();
            playingMolecules = true;
            ourThread = new Thread(this);
            ourThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent){

            switch (motionEvent.getAction() &
                    MotionEvent.ACTION_MASK){
                case MotionEvent.ACTION_UP:
                    if(motionEvent.getX() >= screenWidth / 2 ){
                        Log.i("info","RIGHT");
                    }

            }
            return true;
        }




        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.i("touchEvents","Fling");
            return true;
        }



        /*@Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            gestureText.setText("onScroll");
            return true;
        }*/

    }

    @Override
    protected void onStop(){
        super.onStop();
        while(true){
            gameView.pause();
            mediaPlayer.release();
            break;
        }
        finish();
    }

    @Override
    protected void onResume(){
        super.onResume();
        gameView.resume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        gameView.pause();
    }

    public boolean onKeyDown(int keyCode,KeyEvent keyEvent){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            gameView.pause();
            Intent i = new Intent(this,MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        return false;
    }
}
