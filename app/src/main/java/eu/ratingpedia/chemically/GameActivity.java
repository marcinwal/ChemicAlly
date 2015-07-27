package eu.ratingpedia.chemically;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class GameActivity extends Activity {

    GestureDetector myG;

    MediaPlayer mediaPlayer;
    Canvas canvas;
    Bitmap [] elements = MainActivity.elements; //to avoid loading again
    static int [][] gameGrid; //NOWADDED static
    long lastFrameTime;


    int maxNumberAtomsInMolecule = 10;

    static Molecule playersMolecule; //players molecule scattered all over   NOWADDED static
    static Molecule targetMolecule;  //target molecules                      NOWADDED static

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

    String title,previousTitle;
    String formula,previousFormula;

    static int score = 0;
    static int level = 1;
    static int maxUnlockedLevel = 0; //level which can be loaded from start;depends on previous play
    int maxLevel = 3;  //maximum number of levels


    int fps;
    static Intent i; //NOWADDED static

    //grid where reset button is located
    int[] resetLevelButton = {17,10};
    int[] minusLevelButton = {16,11};
    int[] plusLevelButton =  {18,11};


    boolean won;
    static boolean levelLoaded = false; //NOWADDED

    private Typeface typeFace;

    boolean congratulations = true;
    Handler handler;

    GameView gameView;

    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        handler = new Handler();


        mediaPlayer = MediaPlayer.create(this,R.raw.music2);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        playersMolecule = new Molecule();
        targetMolecule = new Molecule();

        typeFace = Typeface.createFromAsset(getAssets(), "fonts/chockablocknf.ttf");

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
        public void onLongPress(MotionEvent event) {
            int[] position;
            Log.i("LongPress:", "onLongPress: " + event.toString());

            position = findXY(event.getX(),event.getY());
            if ((position[0] == resetLevelButton[0]) && (position[1] == resetLevelButton[1])){
                gameView.resetLevel();
                Log.i("reset level",""+level);
            }

            if ((position[0] == minusLevelButton[0]) && (position[1] == minusLevelButton[1] && level > 1)){
                level--;
                gameView.resetLevel();
                Log.i("minus level",""+level);
            }

            if ((position[0] == plusLevelButton[0]) && (position[1] == plusLevelButton[1]) && level <= maxUnlockedLevel && maxUnlockedLevel != 0){
                level++;
                gameView.resetLevel();
                Log.i("plus level",""+level);
            }

        }

        @Override
        public boolean onFling(MotionEvent e1,MotionEvent e2,float velocityX,float velocityY){
            if(!playersMolecule.isMoving) {
                int[] position;
                int[] position2;
                int direction;

                position = findXY(e1.getX(), e1.getY());
                position2 = findXY(e2.getX(), e2.getY());

                direction = (Math.abs(position[0]-position2[0])>Math.abs(position[1]-position2[1]))?
                            (position[0] > position2[0]? 4: 2 ) :
                            (position[1] > position2[1]? 1: 3 );

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
        useLoadedLevel(loadedBoard, gameGrid);
    }

    private void useLoadedLevel(String loadedBoard, int[][] gameGrid) {
        int numberOfFields = numBlocksWide * numBlocksHigh;
        int atomSymbol;
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
        previousFormula = formula;
        previousTitle = title;
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

        gameGrid = new int[numBlocksWide][numBlocksHigh];

        targetLineIndicator = 2; //where to place the molecule
        numberOfPhases = 8;


        //scaling bitmaps for the screen
        for(int i = 0;i < elements.length;i++){
                elements[i] = Bitmap.createScaledBitmap(elements[i],
                        blockSize - targetLineIndicator, blockSize - targetLineIndicator, false);
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

        public boolean sameMolecule(Molecule targetMolecule){

            boolean same = true;

            if (this.isMoving){
                return false;
            }

            for(int i = 0; i < targetMolecule.numberOfAtoms;i++){
                int xTarget = targetMolecule.atoms[i].posX;
                int yTarget = targetMolecule.atoms[i].posY;
                int elemTarget = targetMolecule.atoms[i].atomIdx;
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

            if (targetMolecule.numberOfAtoms == 0){
                same = false;
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

            loadScore(); //NOWADDED



            dialog = new Dialog(this.getContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.mycustom_dialog);
            dialog.setCancelable(false);


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

            if (won && congratulations){
                showCongratulationsDialog();
            }
        }

        private void resetLevel(){
            congratulations = false;
            resetAtoms();
            setBoard(level);
            congratulations = true;
        }

        private void newLevel(){
            score += targetMolecule.numberOfAtoms * level;
            if (level > maxUnlockedLevel) {
                maxUnlockedLevel = level;
            }
            if (level == maxLevel){
                level = 0;
            }

            level ++;
            updateScore();
            saveScore();
            resetAtoms();
            setBoard(level);
            congratulations = true;
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
            SharedPreferences prefs;
            SharedPreferences.Editor editor;
            String dataName = "ChemicAlly";

            prefs = getSharedPreferences(dataName, MODE_PRIVATE);
            editor = prefs.edit();

            editor.putInt("Score",score);
            editor.putInt("MaxUnlockedLevel",maxUnlockedLevel);
            editor.putInt("Level", level);
            editor.commit();
            Log.e("Saving",""+score+maxUnlockedLevel+level);

        }

        private void loadScore(){
            SharedPreferences prefs;
            String dataName = "ChemicAlly";

            prefs = getSharedPreferences(dataName,MODE_PRIVATE);
            score = prefs.getInt("Score", 1);
            maxUnlockedLevel = prefs.getInt("MaxUnlockedLevel", 1);
            level = prefs.getInt("Level",1);

            Log.e("Loading",""+score+maxUnlockedLevel+level);

        }

        private void updateScore() {
            animateBoom();
        }

        private void animateBoom() {
            Log.e("Animating","BOOM");
        }

        private void showCreditFinals(){
            Log.e("Showing","Credentials");
        }


        private void showCongratulationsDialog(/*final Context context*/){

            handler.post(new Runnable() {



                @Override
                public void run() {

                    won = false;
                    congratulations = false;
                    TextView text1 = (TextView) dialog.findViewById(R.id.textToast1);
                    TextView text2 = (TextView) dialog.findViewById(R.id.textToast2);

                    String show = level == maxLevel? "\nYou finished the game!" : "Well done!!";

                    text1.setText(show);
                    text2.setText(title + ": " + formula);
                    text1.setTextSize(25f);
                    text1.setTextColor(Color.BLACK);
                    text2.setTextSize(25f);
                    text2.setTextColor(Color.BLACK);


                    Button dialogButton = (Button) dialog.findViewById(R.id.buttonDialog);
                    dialogButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            newLevel();
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                }
            });
        }

        private void showCongratulationsPopUp() {
            if (congratulations){
                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        LayoutInflater inflater = getLayoutInflater();
                        View customToastroot = inflater.inflate(R.layout.mycustom_toast,null);
                        Toast toast;
                        toast = Toast.makeText(getApplicationContext(),"Congratulations! Level "+(level-1)+" passed.",Toast.LENGTH_SHORT);
                        toast.setView(customToastroot);

                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.setDuration(Toast.LENGTH_SHORT);

                        TextView text1 = (TextView) customToastroot.findViewById(R.id.textToast1);
                        TextView text2 = (TextView) customToastroot.findViewById(R.id.textToast2);
                        text1.setText("Well done!");
                        text2.setText(previousTitle + "  " + previousFormula);
                        text1.setTextSize(25f);
                        text1.setTextColor(Color.BLACK);
                        text2.setTextSize(25f);
                        text2.setTextColor(Color.BLACK);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                    }
                });
            }
            congratulations = false;

        }

        //drawing walls
        private void drawWalls(){
            paint.setColor(Color.argb(155, 0, 255, 0));//(125,0,255,0)
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
            paint.setTextSize(blockSize * (float) 0.75);
            paint.setColor(Color.BLACK);
            paint.setTypeface(typeFace);
            canvas.drawText("Level " + Integer.toString(level), (leftGap + numBlocksWideBoard - 1) * blockSize, topGap + (blockSize) * 2, paint);
            canvas.drawText("Score", (leftGap + numBlocksWideBoard - 1) * blockSize, topGap + (blockSize) * 3, paint);
            canvas.drawText(Integer.toString(score),  (leftGap + numBlocksWideBoard) * blockSize, topGap + (blockSize) * 4, paint);
            //canvas.drawText("HiScore" + Integer.toString(hiScore), (leftGap + numBlocksWideBoard - 1) * blockSize, topGap + (blockSize) * 5, paint);
        }

        private void controlFPS() {
            long timeThisFrame = System.currentTimeMillis() - lastFrameTime;
            long timeToSleep = 10 - timeThisFrame; //100 25
            if(timeThisFrame > 0){
                fps = (int) (200/timeThisFrame);   //1000 250
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
