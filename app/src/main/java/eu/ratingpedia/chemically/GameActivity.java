package eu.ratingpedia.chemically;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.gesture.Gesture;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;


public class GameActivity extends Activity {

    GestureDetector myG;

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

        myG = new GestureDetector(this,new GestureListener());

        i = new Intent(this,MainActivity.class);

        configureDisplay();

        gameView = new GameView(this);

        setContentView(gameView);

    }

    public boolean onTouchEvent(MotionEvent event){
        myG.onTouchEvent(event);
        return true;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener{

        private int[] findXY(float posX,float posY){
            int x = (int) (posX - leftGap)/blockSize;
            int y = (int) (posY - topGap)/blockSize;
            return new int[]{x,y};
        }

        @Override
        public boolean onFling(MotionEvent e1,MotionEvent e2,float velocityX,float velocityY){
            if(!playersMolecule.isMoving) {
                int[] position = new int[2];
                int[] position2 = new int[2];
                int direction;

                position = findXY(e1.getX(), e1.getY());
                position2 = findXY(e2.getX(), e2.getY());

                direction = (Math.abs(position[0]-position2[0])>Math.abs(position[1]-position2[1]))?
                            (position[0] > position2[0]? 4: 2 ) :
                            (position[1] > position2[1]? 1: 3 );
                // TODO if initial position is ok with element
                int atom = gameGrid[position[0]][position[1]];
                if (atom > -1){ //no empty space -1 nor wall -2
                    playersMolecule.atoms[atom].direction = direction;
                    playersMolecule.isMoving = true;
                }
                return true;
            }
            return false;
        }
    }

    //will be loadingLevels
    private void setBoard(){
        for(int i = 0; i < numBlocksWide;i++) {
            for (int j = 0; j < numBlocksHigh; j++) {
                gameGrid[i][j] = -1;
            }
        }

        for(int i = 0;i < numBlocksWide;i++){  //CHANGE
            gameGrid[i][0] = -2; //wall
            gameGrid[i][numBlocksHighBoard-1] = -2;
            gameGrid[i][numBlocksHigh-1] = -2;
        }

        for(int i = 0;i < numBlocksHighBoard;i++){
            gameGrid[0][i] = -2; //wall
            gameGrid[numBlocksWideBoard-1][i] = -2;
        }

        //funny wall in the middle test
        gameGrid[8][8] = -2;
        gameGrid[8][9] = -2;
        gameGrid[8][10] = -2;
    }

    private void configureDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        topGap = 0;// TODO for the time being
        rightGap = 0;// TODO for the time being the whole screen

        numBlocksWide = 20;
        blockSize = screenWidth / numBlocksWide;
        numBlocksHigh = ((screenHeight - topGap))/blockSize;
        numBlocksWideBoard = numBlocksWide - 4;//TODO minus right gap
        numBlocksHighBoard = numBlocksHigh;

        gameGrid = new int[numBlocksWide][numBlocksHigh]; // CHANGE WAS HighBoard

        targetLineIndicator = 2; //where to place the molecule
        numberOfPhases = 3;


        //scaling bitmaps for the screen
        for(int i = 0;i < elements.length;i++){
            elements[i] = Bitmap.createScaledBitmap(elements[i],
                          blockSize - targetLineIndicator,blockSize-targetLineIndicator,false);
        }
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
        boolean isMoving;

        public Molecule(){
            atoms = new Atom[maxNumberAtomsInMolecule];
            numberOfAtoms = 0;
            isMoving = false;
        }

        //adding atom to the molecule
        public void addAtomToMolecule(Atom atom,boolean effectGrid){
            if(numberOfAtoms < maxNumberAtomsInMolecule ){
                if (effectGrid){
                    gameGrid[atom.posX][atom.posY] = numberOfAtoms;
                }
                atoms[numberOfAtoms++] = atom;
            }
        }

        public void moveMoleculeAtom(int atomIdx,int deltaX,int deltaY){
            int posX = atoms[atomIdx].posX;
            int posY = atoms[atomIdx].posY;

            if(((posX+deltaX < numBlocksWideBoard && deltaX > 0) ||
                    (posY+deltaY < numBlocksHighBoard && deltaY > 0) ||
                    (posX+deltaX >= 0 && deltaX < 0) ||
                    (posY+deltaY >= 0 && deltaY < 0)) &&
                    gameGrid[posX+deltaX][posY+deltaY] == -1) { //no obstacles

                gameGrid[posX][posY] = -1;                 //moved the element
                atoms[atomIdx].changeXY(deltaX, deltaY);
                gameGrid[posX+deltaX][posY+deltaY] = atomIdx; //placing on the grid
            }else{
                atoms[atomIdx].direction = 0;
                this.isMoving = false;
            }
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

        volatile boolean playingMolecules;
        Paint paint;


        public GameView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            getBoard();
            getMoleculesPlayer();
            getMoleculesTarget();
        }

        private void getMoleculesTarget() {
            //TODO load it from the file
            Atom atomO = new Atom(8,8,7);
            Atom atomH1 = new Atom(6,7,8);
            Atom atomH2 = new Atom(6,9,8);
            targetMolecule.addAtomToMolecule(atomO, false);
            targetMolecule.addAtomToMolecule(atomH1, false);
            targetMolecule.addAtomToMolecule(atomH2, false);
        }

        private void getMoleculesPlayer() {
            Atom atomO = new Atom(8,1,1);
            Atom atomH1 = new Atom(6,3,7);
            Atom atomH2 = new Atom(6,8,6);
            playersMolecule.addAtomToMolecule(atomO,true);
            playersMolecule.addAtomToMolecule(atomH1,true);
            playersMolecule.addAtomToMolecule(atomH2,true);
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
            paint.setColor(Color.argb(125, 0, 255, 0));
            for(int i = 0; i < numBlocksWide;i++) { //CHANGE
                for (int j = 0; j < numBlocksHigh; j++) { //CHANGE
                    if (gameGrid[i][j] == -2) {
                        canvas.drawBitmap(elements[16],leftGap + i * blockSize,topGap + j * blockSize,paint);
                    }
                }
            }
            //lower line
            for(int i = 0 ; i < numBlocksWide;i++){
                canvas.drawBitmap(elements[16],leftGap + i * blockSize,topGap + numBlocksHigh * blockSize,paint);//chnage
            }

        }

        private void drawTarget(){
            for(int i = 0 ; i < targetMolecule.numberOfAtoms;i++){

                paint.setColor(Color.argb(255, 255, 255, 255));
                canvas.drawRect(leftGap+targetMolecule.atoms[i].posX * blockSize,
                        topGap + targetMolecule.atoms[i].posY * blockSize,
                        leftGap + targetMolecule.atoms[i].posX * blockSize + blockSize,
                        topGap + targetMolecule.atoms[i].posY * blockSize + blockSize, paint);

                paint.setColor(Color.argb(255, 0, 0, 0));
                canvas.drawRect(leftGap + targetMolecule.atoms[i].posX * blockSize + targetLineIndicator,
                        topGap + targetMolecule.atoms[i].posY * blockSize + targetLineIndicator,
                        leftGap + targetMolecule.atoms[i].posX * blockSize + blockSize - targetLineIndicator,
                        topGap + targetMolecule.atoms[i].posY * blockSize + blockSize - targetLineIndicator, paint);
            }
        }

        private void drawGame() {
            if(ourHolder.getSurface().isValid()){
                canvas = ourHolder.lockCanvas();
                canvas.drawColor(Color.DKGRAY);
                drawWalls();
                drawTarget();
                paint.setColor(Color.argb(255, 255, 255, 255));
                for(int i = 0; i < playersMolecule.numberOfAtoms;i++){
                    canvas.drawBitmap(elements[playersMolecule.atoms[i].atomIdx],
                            leftGap + playersMolecule.atoms[i].posX * blockSize+targetLineIndicator,
                            topGap + playersMolecule.atoms[i].posY * blockSize+targetLineIndicator,paint);
                }
                drawText();
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        private void drawText() {
            paint.setTextSize(blockSize);
            paint.setColor(Color.argb(255,0,0,155));
            canvas.drawText("Level",leftGap+numBlocksWideBoard*blockSize,topGap+blockSize*2,paint);
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
    protected void onPause() {
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
