package com.example.bug;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.widget.Toast;

import static com.example.bug.Assets.bug;
import static com.example.bug.Bug.BugState.Dead;

public class MainThread extends Thread {



    private SurfaceHolder holder;
    private Handler handler;		// required for running code in the UI thread
    private boolean isRunning = false;
    Context context;
    Paint paint;
    int touchx, touchy;	// x,y of touch event
    boolean touched;	// true if touch happened
    boolean data_initialized;
    private static final Object lock = new Object();

    public MainThread (SurfaceHolder surfaceHolder, Context context) {
        holder = surfaceHolder;
        this.context = context;
        handler = new Handler();
        data_initialized = false;
        touched = false;
    }

    public void setRunning(boolean b) {
        isRunning = b;	// no need to synchronize this since this is the only line of code to writes this variable
    }

    // Set the touch event x,y location and flag indicating a touch has happened
    public void setXY (int x, int y) {
        synchronized (lock) {
            touchx = x;
            touchy = y;
            this.touched = true;
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            // Lock the canvas before drawing
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                // Perform drawing operations on the canvas
                render(canvas);
                // After drawing, unlock the canvas and display it
                holder.unlockCanvasAndPost (canvas);
            }
        }
    }

    // Loads graphics, etc. used in game
    private void loadData (Canvas canvas) {
        Bitmap bmp;
        int newWidth, newHeight;
        float scaleFactor;

        // Create a paint object for drawing vector graphics
        paint = new Paint();

        // Load score bar
        // ADD CODE HERE

        // Load food bar
        bmp = BitmapFactory.decodeResource (context.getResources(), R.drawable.foodbar);
        // Compute size of bitmap needed (suppose want height = 10% of screen height)
        newHeight = (int)(canvas.getHeight() * 0.1f);
        // Scale it to a new size
        Assets.foodbar = Bitmap.createScaledBitmap (bmp, canvas.getWidth(), newHeight, false);
        // Delete the original
        bmp = null;

        // Load roach1















        bmp = BitmapFactory.decodeResource (context.getResources(), R.drawable.ant_walk1);
        // Compute size of bitmap needed (suppose want width = 20% of screen width)
        newWidth = (int)(canvas.getWidth() * 0.2f);
        // What was the scaling factor to get to this?
        scaleFactor = (float)newWidth / bmp.getWidth();
        // Compute the new height
        newHeight = (int)(bmp.getHeight() * scaleFactor);
        // Scale it to a new size
        Assets.ant1 = Bitmap.createScaledBitmap (bmp, newWidth, newHeight, false);
        // Delete the original
        bmp = null;

        // Load the other bitmaps similarly
        bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.ant_walk2);
        Assets.ant2 = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, false);
        bmp = null;

        // Load roach3 (dead bug)
        bmp = BitmapFactory.decodeResource (context.getResources(), R.drawable.ant_dead);
        // Compute size of bitmap needed (suppose want width = 20% of screen width)
        newWidth = (int)(canvas.getWidth() * 0.2f);
        // What was the scaling factor to get to this?
        scaleFactor = (float)newWidth / bmp.getWidth();
        // Compute the new height
        newHeight = (int)(bmp.getHeight() * scaleFactor);
        // Scale it to a new size
        Assets.ant3 = Bitmap.createScaledBitmap (bmp, newWidth, newHeight, false);
        // Delete the original
        bmp = null;

        // Create a bug
        bug[0] =new Bug(Dead);
        bug[1] =new Bug(Dead);
        bug[2] =new Bug(Dead);
    }

    // Load specific background screen
    private void loadBackground (Canvas canvas, int resId) {
        // Load background
        Bitmap bmp = BitmapFactory.decodeResource (context.getResources(), resId);
        // Scale it to fill entire canvas
        Assets.background = Bitmap.createScaledBitmap (bmp, canvas.getWidth(), canvas.getHeight(), false);
        // Delete the original
        bmp = null;
    }

    private void render (Canvas canvas) {
        int i, x, y;

        if (! data_initialized) {
            loadData(canvas);
            data_initialized = true;
        }

        switch (Assets.state) {
            case GettingReady:
                loadBackground (canvas, R.drawable.grass);
                // Draw the background screen
                canvas.drawBitmap (Assets.background, 0, 0, null);
                // Play a sound effect
                Assets.sp.play(Assets.sound_getready, 1, 1, 1, 0, 1);
                // Start a timer
                Assets.gameTimer = System.nanoTime() / 1000000000f;
                // Go to next state
                Assets.state = Assets.GameState.Starting;
                break;
            case Starting:
                // Draw the background screen
                canvas.drawBitmap (Assets.background, 0, 0, null);
                // Has 3 seconds elapsed?
                float currentTime = System.nanoTime() / 1000000000f;
                if (currentTime - Assets.gameTimer >= 3)
                    // Goto next state
                    Assets.state = Assets.GameState.Running;
                break;
            case Running:
                // Draw the background screen
                canvas.drawBitmap (Assets.background, 0, 0, null);
                // Draw the score bar at top of screen
                // ADD CODE HERE
                // Draw the foodbar at bottom of screen
                canvas.drawBitmap (Assets.foodbar, 0, canvas.getHeight()-Assets.foodbar.getHeight(), null);
                // Draw one circle for each life at top right corner of screen
                // Let circle radius be 5% of width of screen
                int radius = (int)(canvas.getWidth() * 0.05f);
                int spacing = 8; // spacing in between circles
                x = canvas.getWidth() - radius - spacing;	// coordinates for rightmost circle to draw
                y = radius + spacing;
                for (i=0; i<Assets.livesLeft; i++) {
                    paint.setColor(Color.GREEN);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(x, y, radius, paint);
                    paint.setColor(Color.BLACK);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawCircle(x, y, radius, paint);
                    // Reposition to draw the next circle to the left
                    x -= (radius*2 + spacing);
                }

                // Process a touch
                if (touched) {
                    // Set touch flag to false since we are processing this touch now
                    touched = false;
                    // See if this touch killed a bug
                    boolean bugKilled1 = bug[0].touched(canvas, touchx, touchy);
                    if (bugKilled1)
                        Assets.sp.play(Assets.sound_squish1, 1, 1, 1, 0, 1);
                    else
                        Assets.sp.play(Assets.sound_thump, 1, 1, 1, 0, 1);
                    boolean bugKilled2 = bug[1].touched(canvas, touchx, touchy);
                    if (bugKilled2)
                        Assets.sp.play(Assets.sound_squish1, 1, 1, 1, 0, 1);
                    else
                        Assets.sp.play(Assets.sound_thump, 1, 1, 1, 0, 1);
                    boolean bugKilled3 = bug[2].touched(canvas, touchx, touchy);
                    if (bugKilled3)
                        Assets.sp.play(Assets.sound_squish1, 1, 1, 1, 0, 1);
                    else
                        Assets.sp.play(Assets.sound_thump, 1, 1, 1, 0, 1);
                }

                // Draw dead bugs on screen
                bug[0].drawDead(canvas);
                // Move bugs on screen
                bug[0].move(canvas);
                // Bring a dead bug to life?
                bug[0].birth(canvas);
                // Draw dead bugs on screen
                bug[1].drawDead(canvas);
                // Move bugs on screen
                bug[1].move(canvas);
                // Bring a dead bug to life?
                bug[1].birth(canvas);
                // Draw dead bugs on screen
                bug[2].drawDead(canvas);
                // Move bugs on screen
                bug[2].move(canvas);
                // Bring a dead bug to life?
                bug[2].birth(canvas);

                // ADD MORE CODE HERE TO PLAY GAME


                // Are no lives left?
                if (Assets.livesLeft == 0)
                    // Goto next state
                    Assets.state = Assets.GameState.GameEnding;
                break;
            case GameEnding:
                // Show a game over message
                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(context, "Game Over", Toast.LENGTH_SHORT).show();
                    }
                });
                // Goto next state
                Assets.state = Assets.GameState.GameOver;
                break;
            case GameOver:
                // Fill the entire canvas' bitmap with 'black'
                canvas.drawColor(Color.BLACK);
                break;
        }
    }
}
