package eu.ratingpedia.chemically;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class GameActivity extends Activity {

    MediaPlayer mediaPlayer;
    Bitmap [] elements = MainActivity.elements; //to avoid loading again
    int [] selectedElements = MainActivity.selectedElements;
    int maxNumberAtomsInMolecule = 10;

    Molecule playersMolecule;
    Molecule targetMolecule;

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

        //compares to molecules if they are equal, atoms must be placed in the same relative setup
        public boolean sameMolecule(Molecule molecule){
            boolean same = true;
            for(int i = 0; i < molecule.atoms.length;i++){

            }

        }

    }


    private class GameView extends SurfaceView implements Runnable {

        Thread ourThread;
        SurfaceHolder ourHolder;
        volatile boolean movingMolecules;
        Paint paint;


        public GameView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();
        }

        @Override
        public void run() {

        }
    }
}
