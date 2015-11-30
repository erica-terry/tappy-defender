package com.ericaterry.tappydefender;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;

//pg 56
public class TDView extends SurfaceView implements Runnable{

    private int screenX;
    private int screenY;
    volatile boolean playing;
    Thread gameThread = null;
    // Game objects
    private PlayerShip player;
    public EnemyShip enemy1;
    public EnemyShip enemy2;
    public EnemyShip enemy3;
    public EnemyShip enemy4;
    public EnemyShip enemy5;
    // For drawing
    private Paint paint;
    private Canvas canvas;
    private SurfaceHolder ourHolder;
    // Make some random space dust
    public ArrayList<SpaceDust> dustList = new ArrayList<SpaceDust>();
    private float distanceRemaining;
    private long timeTaken;
    // TODO make a double and truncate to 2 decimal places
    private long timeStarted;
    private long fastestTime;
    private Context context;
    private boolean gameEnded;
    private SoundPool soundPool;
        int start = -1;
        int bump = -1;
        int destroyed = -1;
        int win = -1;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;


    public TDView(Context context, int x, int y) {
        super(context);
        this.context = context;

        // This SoundPool is deprecated but don't worry
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        try {
            // Create objects of the 2 required classes
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // create our three fx in memory ready for use
            descriptor = assetManager.openFd("start.ogg");
            start = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("win.ogg");
            win = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("bump.ogg");
            bump = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("destroyed.ogg");
            bump = soundPool.load(descriptor, 0);

        }catch (IOException e) {
            // Print an error message to the console
            Log.e("error", "failed to load sound files");
        }

        screenX = x;
        screenY = y;

        prefs = context.getSharedPreferences("HiScores", Context.MODE_PRIVATE);
        editor = prefs.edit();
        fastestTime = prefs.getLong("fastestTime", 1000000);

        // Initialize our drawing objects
        ourHolder = getHolder();
        paint = new Paint();

        startGame();

        // Update the player
        player.update();
    }


    private void startGame() {
        // Initialize game objects
        player = new PlayerShip(context, screenX, screenY);
        enemy1 = new EnemyShip(context, screenX, screenY);
        enemy2 = new EnemyShip(context, screenX, screenY);
        enemy3 = new EnemyShip(context, screenX, screenY);

        if(screenX > 1000){
            enemy4 = new EnemyShip(context, screenX, screenY);
        }
        if(screenX > 1200){
            enemy5 = new EnemyShip(context, screenX, screenY);
        }

        int numSpecs = 40;
        for (int i = 0; i < numSpecs; i++) {
            // Where will the dust spawn?
            SpaceDust spec = new SpaceDust(screenX, screenY);
            dustList.add(spec);
        }

        // Reset time and distance
        distanceRemaining = 10000;// 10 km
        timeTaken = 0;

        // Get start time
        timeStarted = System.currentTimeMillis();

        gameEnded = false;

        soundPool.play(start, 1, 1, 0, 0, 1);
    }


    @Override
    public void run() {
        while(playing)
        {
            update();
            draw();
            control();
        }
    }

    private void control() {
        try {
            gameThread.sleep(17);
        } catch (InterruptedException e) {

        }
    }

    private void draw() {
        if(ourHolder.getSurface().isValid()) {

            // First we lock the area of memory we will be drawing to
            canvas = ourHolder.lockCanvas();

            // Rub out the last frame
            canvas.drawColor(Color.argb(255, 0, 0, 0));

            // White specs of dust
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Draw the dust from our arrayList
            for(SpaceDust sd : dustList)
                canvas.drawPoint(sd.getX(), sd.getY(), paint);

            // Draw the player
            canvas.drawBitmap(player.getBitmap(), player.getX(), player.getY(), paint);
            // Draw the enemies
            canvas.drawBitmap(enemy1.getBitmap(), enemy1.getX(), enemy1.getY(), paint);
            canvas.drawBitmap(enemy2.getBitmap(), enemy2.getX(), enemy2.getY(), paint);
            canvas.drawBitmap(enemy3.getBitmap(), enemy3.getX(), enemy3.getY(), paint);

            if(screenX > 1000) {
                canvas.drawBitmap(enemy4.getBitmap(), enemy4.getX(), enemy4.getY(), paint);
            }
            if(screenX > 1200) {
                canvas.drawBitmap(enemy5.getBitmap(), enemy5.getX(), enemy5.getY(), paint);
            }

            // TODO fix text for different screen sizes
            if(!gameEnded) {

                // Draw the hud
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(50);
                canvas.drawText("Fastest: " + formatTime(fastestTime) + "s", 10, 50, paint);
                canvas.drawText("Time: " + formatTime(timeTaken) + "s", screenX / 2, 50, paint);
                canvas.drawText("Distance: " + distanceRemaining / 1000 + " KM", screenX / 3, screenY - 20, paint);
                canvas.drawText("Shield: " + player.getShieldStrength(), 10, screenY - 20, paint);
                canvas.drawText("Speed: " + player.getSpeed() * 60 + " MPS", (screenX / 3) * 2, screenY - 20, paint);
            } else {

                // Show pause screen
                paint.setTextSize(80);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Game Over", screenX / 2, 100, paint);
                paint.setTextSize(50);
                canvas.drawText("Fastest: " + formatTime(fastestTime) + "s", screenX / 2, 160, paint);
                canvas.drawText("Time: " + formatTime(timeTaken) + "s", screenX / 2, 220, paint);
                canvas.drawText("Distance remaining: " + distanceRemaining/1000 + " KM",screenX/2, 280, paint);
                paint.setTextSize(80);
                canvas.drawText("Tap to replay!", screenX/2, 400, paint);
            }

            // Unlock and draw the scene
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void update() {
        // Collision detection on new positions
        // Before move because we are testing last frames
        // position which has just been drawn
        boolean hitDetected = false;
        if(Rect.intersects(player.getHitbox(), enemy1.getHitbox())) {
            hitDetected = true;
            enemy1.setX(-enemy1.getHitbox().width());
        }
        if(Rect.intersects(player.getHitbox(), enemy2.getHitbox())) {
            hitDetected = true;
            enemy2.setX(-enemy2.getHitbox().width());
        }
        if(Rect.intersects(player.getHitbox(), enemy3.getHitbox())) {
            hitDetected = true;
            enemy3.setX(-enemy3.getHitbox().width());
        }
        if(screenX > 1000){
            if(Rect.intersects(player.getHitbox(), enemy4.getHitbox())){
                hitDetected = true;
                enemy4.setX(-enemy4.getHitbox().width());
            }
        }
        if(screenX > 1200){
            if(Rect.intersects(player.getHitbox(), enemy5.getHitbox())){
                hitDetected = true;
                enemy5.setX(-enemy5.getHitbox().width());
            }
        }

        if(hitDetected) {
            soundPool.play(bump, 1, 1, 0, 0, 1);
            player.reduceShieldStrength();
                if(player.getShieldStrength() < 0) {
                    soundPool.play(destroyed, 1, 1, 0, 0, 1);
                    gameEnded = true;
                }
        }

        // Update the player
        player.update();

        // Update the enemies
        enemy1.update(player.getSpeed());
        enemy2.update(player.getSpeed());
        enemy3.update(player.getSpeed());
        if(screenX > 1000) {
            enemy4.update(player.getSpeed());
        }
        if(screenX > 1200) {
            enemy5.update(player.getSpeed());
        }

        for(SpaceDust sd : dustList) {
            sd.update(player.getSpeed());
        }

        if(!gameEnded) {
            // subtract distance to home planet based on current speed
            distanceRemaining -= player.getSpeed();

            // How long has the player been flying
            timeTaken = System.currentTimeMillis() - timeStarted;
        }

        // Completed the game!
        if(distanceRemaining < 0) {
            soundPool.play(win, 1, 1, 0, 0, 1);
            // check for new fastest time
            if(timeTaken < fastestTime) {
                fastestTime = timeTaken;
                editor.putLong("fastestTime", fastestTime);
                editor.commit();
            }

            // avoid ugly negative numbers in the HUD
            distanceRemaining = 0;

            // Now end the game
            gameEnded = true;
        }
    }

    //Clean up our thread if the game is interrupted or the player quits
    public void pause() {
        playing = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {

        }
    }

    // Make a new thread and start it
    // Execution moves to our R
    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // SurfaceView allows us to handle the onTouchEvent
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        // There are many different events in MotionEvent
        // We care about just 2 - for now.
        switch(motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            // Has the player lifted their finger up?
            case MotionEvent.ACTION_UP:
                player.stopBoosting();
                break;

            // Has the player touched the screen?
            case MotionEvent.ACTION_DOWN:
                player.setBoosting();
                // If we are currently on the pause screen, start a new game
                if(gameEnded) {
                    startGame();
                }
                break;
        }
        return true;
    }

    private String formatTime(long time) {
        long seconds = (time) / 1000;
        long thousandths = (time) - (seconds * 1000);
        String strThousandths = "" + thousandths;
        if(thousandths < 100) {strThousandths = "0" + thousandths;}
        if(thousandths < 100) {strThousandths = "0" + thousandths;}
        String stringTime = "" + seconds + "." + strThousandths;
        return stringTime;

    }
}
