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
import android.graphics.Typeface;
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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

    String title;
    String formula;

    int score;
    int hiScore;
    int level=1;

    int fps;
    Intent i;

    boolean won;

    private Typeface typeFace;

    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaPlayer = MediaPlayer.create(this,R.raw.music2);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        playersMolecule = new Molecule();
        targetMolecule = new Molecule();

        typeFace = Typeface.createFromAsset(getAssets(),"fonts/pacfont.ttf");

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
                int[] position =  new int[2];
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


    private String loadLevel(int level){

        String levelS = "level"+Integer.toString(level);

        InputStream inputStream = getResources().openRawResource(getResources().getIdentifier(levelS, "raw", getPackageName()));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try{
            i = inputStream.read();
            while (i != -1){
                if (i != 13 && i !=10) {
                    byteArrayOutputStream.write(i);
                }
                i = inputStream.read();
            }
            inputStream.close();

        }catch(IOException e){
            e.printStackTrace();
        }

        return String.valueOf(byteArrayOutputStream);

    }

    private void setBoard(int level){

        String loadedBoard;
        loadedBoard = loadLevel(level);
        Log.i("level",loadedBoard);
        useLoadedLevel(loadedBoard,gameGrid);

    }

    private void useLoadedLevel(String loadedBoard, int[][] gameGrid) {
        int numberOfFields = numBlocksWide * numBlocksHigh;
        int atomSymbol = -1;
        int targetSymbol;
        for (int i = 0; i < numberOfFields;i++){
            atomSymbol = -1;
            targetSymbol = -1;
            int element = loadedBoard.charAt(i);
            switch(element){
                case 'x':
                    atomSymbol = -2;
                    break;
                case ' ':
                    atomSymbol = -1;
                    break;
                default:
                    if ( element >= 'a' && element <= 'z'){
                        atomSymbol = element - 'a';
                    }
                    if (element >= 'A' && element <= 'Z'){
                        targetSymbol = element - 'A';
                    }
            }
            int x = i % numBlocksWide;
            int y = i / numBlocksWide;

            gameGrid[x][y] = atomSymbol;

            if (atomSymbol > 0 ){
                Atom atom = new Atom(atomSymbol,x,y);
                playersMolecule.addAtomToMolecule(atom,true);
            }
            if (targetSymbol != -1){
                Atom atom = new Atom(targetSymbol,x,y);
                targetMolecule.addAtomToMolecule(atom,false);
            }
            playersMolecule.isMoving = false;
        }

        //reading the formula
        int i = numberOfFields;
        formula = "";
        char current = loadedBoard.charAt(i);
        while (current != ' '){
            formula += current;
            i++;
            current = loadedBoard.charAt(i);
        }
        //reading the name
        i++;
        title = "";
        while(i < loadedBoard.length() ){
            current = loadedBoard.charAt(i);
            title += current;
            i++;
        }
        Log.i("Title",title);
        Log.i("Formula",formula);
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
        /*numBlocksHigh = ((screenHeight - topGap))/blockSize; */
        numBlocksHigh = 12;
        numBlocksWideBoard = numBlocksWide - 4;//TODO minus right gap
        numBlocksHighBoard = numBlocksHigh;

        gameGrid = new int[numBlocksWide][numBlocksHigh]; // CHANGE WAS HighBoard

        targetLineIndicator = 2; //where to place the molecule
        numberOfPhases = 8;


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

        public void atomStop() {
            this.direction = 0;
            this.phase = 0;
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

        public void stopAtom(int number){
            this.atoms[number].atomStop();
            this.isMoving = false;
        }

        public boolean sameMolecule(Molecule molecule){

            boolean same = true;

            for(int i = 0; i < molecule.numberOfAtoms;i++){
                int xTarget = molecule.atoms[i].posX;
                int yTarget = molecule.atoms[i].posY;
                int elemTarget = molecule.atoms[i].atomIdx;
                int atomIn = gameGrid[xTarget][yTarget];
                if (gameGrid[xTarget][yTarget] != -1){
                    if(this.atoms[atomIn].atomIdx != elemTarget){
                        same = false;
                        break;
                    }
                }else{
                    same = false;
                    break;
                }

            }

            if (same){
                Log.i("Winner of Level:",""+level);
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
            setBoard(level);
        }

        @Override
        public void run() {
            while(playingMolecules){
                updateGame();
                drawGame();
                controlFPS();
            }
        }


        private void updateGame() {

            for(int i = 0;i < playersMolecule.numberOfAtoms;i++){
                if (playersMolecule.atoms[i].direction != 0){
                    playersMolecule.atoms[i].phase++;
                    if (playersMolecule.atoms[i].phase >= numberOfPhases){
                        playersMolecule.atoms[i].phase = 0; //all phases
                        switch(playersMolecule.atoms[i].direction){
                            case 1:
                                playersMolecule.moveMoleculeAtom(i, 0, -1);//going up
                                break;
                            case 2:
                                playersMolecule.moveMoleculeAtom(i, 1, 0);//going right
                                break;
                            case 3:
                                playersMolecule.moveMoleculeAtom(i, 0, 1);//going down
                                break;
                            case 4:
                                playersMolecule.moveMoleculeAtom(i, -1, 0);//going left
                                break;
                        }
                    }
                    break;//only one molecule can move at the time
                }
            }

            won = playersMolecule.sameMolecule(targetMolecule);
            if (won){
                level ++;
                congs();
                updateScore();
                saveScore();
                resetAtoms();
                setBoard(level);

            }


        }

        private void resetAtoms() {
            for(int i = 0; i < playersMolecule.numberOfAtoms;i++){
                playersMolecule.atoms[i] = null;
            }
            playersMolecule.numberOfAtoms = 0;
            for(int i = 0; i < targetMolecule.numberOfAtoms;i++){
                targetMolecule.atoms[i] = null;
            }
            targetMolecule.numberOfAtoms = 0;
        }

        private void saveScore() {

        }

        private void updateScore() {
            animateBoom();
        }

        private void animateBoom() {
        }

        private void congs() {

        }

        //drawing walls
        private void drawWalls(){
            paint.setColor(Color.argb(125, 0, 255, 0));
            for(int i = 0; i < numBlocksWide;i++) {
                for (int j = 0; j < numBlocksHigh; j++) {
                    if (gameGrid[i][j] == -2) {
                        canvas.drawBitmap(elements[16],leftGap + i * blockSize,topGap + j * blockSize,paint);
                    }
                }
            }
            //lower line
            for(int i = 0 ; i < numBlocksWide;i++){
                canvas.drawBitmap(elements[16], leftGap + i * blockSize, topGap + numBlocksHigh * blockSize, paint);
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

                    int x = playersMolecule.atoms[i].posX;
                    int y = playersMolecule.atoms[i].posY;
                    int phase = playersMolecule.atoms[i].phase;
                    Bitmap bitmap = elements[playersMolecule.atoms[i].atomIdx];


                    switch(playersMolecule.atoms[i].direction){
                        case 1: //moving up
                                if(gameGrid[x][y-1] == -1) {
                                    canvas.drawBitmap(bitmap,
                                            leftGap + x * blockSize + targetLineIndicator,
                                            topGap + y * blockSize -
                                                    blockSize * phase / numberOfPhases
                                                    + targetLineIndicator, paint);
                                }else{
                                    playersMolecule.stopAtom(i);
                                }
                                break;
                        case 2: //moving right
                                if (gameGrid[x+1][y] == -1) {
                                    canvas.drawBitmap(bitmap, leftGap + x * blockSize + targetLineIndicator +
                                            blockSize * phase / numberOfPhases, topGap + y * blockSize +
                                            targetLineIndicator, paint);
                                }else{
                                    playersMolecule.stopAtom(i);
                                }
                                break;
                        case 3: //moving down
                                if(gameGrid[x][y+1] == -1) {
                                    canvas.drawBitmap(bitmap,
                                        leftGap + x * blockSize + targetLineIndicator,
                                        topGap + y * blockSize +
                                                blockSize * phase / numberOfPhases
                                                + targetLineIndicator, paint);
                                }else{
                                    playersMolecule.stopAtom(i);
                                }
                                break;
                        case 4:
                                if (gameGrid[x-1][y] == -1) {
                                    canvas.drawBitmap(bitmap,
                                            leftGap + x * blockSize + targetLineIndicator -
                                                    blockSize * phase / numberOfPhases,
                                            topGap + y * blockSize
                                                    + targetLineIndicator, paint);
                                }else{
                                    playersMolecule.stopAtom(i);
                                }
                                break;
                    }

                    if (playersMolecule.atoms[i].direction == 0) {
                        canvas.drawBitmap(bitmap,
                                leftGap + x * blockSize + targetLineIndicator,
                                topGap + y * blockSize + targetLineIndicator, paint);
                    }
                }
                    drawText();
                    ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        private void drawText() {
            paint.setTextSize(blockSize);
            paint.setColor(Color.argb(255, 0, 0, 155));
            paint.setTypeface(typeFace);
            canvas.drawText("Level",leftGap+numBlocksWideBoard*blockSize,topGap+blockSize*2,paint);
        }

        private void controlFPS() {
            long timeThisFrame = System.currentTimeMillis() - lastFrameTime;
            long timeToSleep = 25 - timeThisFrame; //100
            if(timeThisFrame > 0){
                fps = (int) (250/timeThisFrame);   //1000
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
