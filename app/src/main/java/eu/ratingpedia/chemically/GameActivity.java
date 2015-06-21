package eu.ratingpedia.chemically;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;


public class GameActivity extends Activity {

    MediaPlayer mediaPlayer;
    Bitmap [] elements = MainActivity.elements; //to avoid loading again
    int [] selectedElements = MainActivity.selectedElements;
    int [][] gameGrid;

    int maxNumberAtomsInMolecule = 10;

    Molecule playersMolecule; //players molecule scattered all over
    Molecule targetMolecule;  //target molecules

    int screenWidth = MainActivity.screenWidth;
    int screenHeight = MainActivity.screenHeight;
    int topGap;
    int rightGap;

    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;
    int numBlocksWideBoard;
    int numBlocksHighBoard;

    int score;
    int hiScore;

    int fps;
    Intent i;

    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaPlayer = MediaPlayer.create(this,R.raw.music2);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        i = new Intent(this,MainActivity.class);

        configureDisplay();

        gameView = new GameView(this);

        setContentView(gameView);
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

        //scaling bitmaps for the screen
        for(int i = 0;i < elements.length;i++){
            elements[i] = Bitmap.createScaledBitmap(elements[i],blockSize,blockSize,false);
        }

    }

    public class Atom {

        int atomIdx,posX,posY,direction;

        public Atom(int atomIdx,int posX,int posY){
            this.atomIdx = atomIdx;
            this.posX = posX;
            this.posY = posY;
            this.direction = 0; // not moving 1,2,3,4
        }

        public void changeXY(int deltaX,int deltaY){
            this.posX += deltaX;
            this.posY += deltaY;
        }
    }

    public class Molecule {
        Atom [] atoms;
        int numberOfMolecules;

        public Molecule(){
            atoms = new Atom[maxNumberAtomsInMolecule];
            numberOfMolecules = 0;
        }

        //adding atom to the molecule
        public void addAtomToMolecule(Atom atom){
            if(numberOfMolecules < maxNumberAtomsInMolecule ){
                atoms[numberOfMolecules++] = atom;
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
        }

        private void getMoleculesTarget() {
            //TODO load it from the file
            Atom atom = new Atom(0,0,0);
            targetMolecule.addAtomToMolecule(atom);
        }

        private void getMoleculesPlayer() {
            Atom atom = new Atom(0,0,0);
            playersMolecule.addAtomToMolecule(atom);
            Random intRandom = new Random();
            int x = intRandom.nextInt(5);
            int y = intRandom.nextInt(5);
            playersMolecule.moveMoleculeAtom(0, x, y);
        }

        //loading the board and setting the grid
        private void getBoard() {

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

        }

        private void drawGame() {

        }

        private void controlFPS() {

        }
    }
}
