package br.neatfire.twodevpong;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import br.neatfire.twodevpong.interfaces.RunningGameCallbacks;
import br.neatfire.twodevpong.model.GameController;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class GameActivity extends AppCompatActivity implements RunningGameCallbacks {

    private static final String TAG = GameActivity.class.getSimpleName();

    protected ViewGroup anchorView;

    protected void hideControls() {
        Window w = getWindow();
        if (w != null) w.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        android.app.ActionBar ab = getActionBar();
        if (ab != null) ab.hide();
        if (w != null) w.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        anchorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE |
                View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    protected View.OnLayoutChangeListener layoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            onLayoutReady();
            anchorView.removeOnLayoutChangeListener(this);
        }
    };

    private Handler game_handler;
    protected ImageView shadow;
    protected ImageView ball;
    protected ImageView bar;
    protected TextView score;
    boolean my_turn;

    String you;
    String opponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        anchorView = (ViewGroup)findViewById(R.id.anchorView);
        anchorView.addOnLayoutChangeListener(layoutChangeListener);
        hideControls();

        shadow = (ImageView)findViewById(R.id.game_ball_shadow);
        ball = (ImageView)findViewById(R.id.game_ball);
        bar = (ImageView)findViewById(R.id.game_bar);
        score = (TextView)findViewById(R.id.game_score);

        my_turn = false;
        game_handler = new Handler(); // game logic handler goes on UI thread (seems ok for me)
        Intent i = getIntent();
        you = i.getStringExtra("you").split(" ", 2)[0]; // only first name
        opponent = i.getStringExtra("opponent").split(" ", 2)[0];
        setTitle(you + " vs " + opponent);
    }

    GameController mGameController;

    @Override
    public void onGameStopped() {
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGameController = GameController.getInstance(this, null);
        mGameController.registerRunningGameCallbacks(this);
        anchorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ((MotionEvent.ACTION_DOWN == event.getAction() ||
                        MotionEvent.ACTION_MOVE == event.getAction()) &&
                        ((100 * event.getY() / anchorView.getHeight()) > 70)) { // only if touching below 70% of screen
                    bar_p = (int) (100 * event.getX() / anchorView.getWidth()); // bar_p in %
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        my_turn = false;
        mGameController.leaveGame();
        finish();
    }

    private void onLayoutReady() {
        mGameController.im_ready();
    }

    @Override
    public void updateOpponentBall(int x_p) {
        this.x_p = x_p;
        game_handler.post(updateUITask);
    }

    private Runnable lostControlUIupdateTask = new Runnable() {
        @Override
        public void run() {
            ball.setVisibility(View.INVISIBLE);
            updateUI();
            shadow.setVisibility(View.VISIBLE);
        }
    };

    private Runnable gotControlUIupdateTask = new Runnable() {
        @Override
        public void run() {
            shadow.setVisibility(View.INVISIBLE);
            updateUI();
            ball.setVisibility(View.VISIBLE);
        }
    };

    private boolean update_score = false;
    protected int score_mine = 0;
    protected int score_opponent = 0;

    @Override
    public void scoreForYou() {
        score_mine++;
        update_score = true;
        game_handler.post(updateUITask);
    }

    @Override
    public void gotBallControl(int x_p, int x_v, int y_v) {
        Log.d(TAG, String.format("Got ball control x(%d) v(%d,%d)", x_p, x_v, y_v));
        this.x_p = x_p;
        this.y_p = 5;
        this.x_v = x_v;
        this.y_v = y_v;
        my_turn = true;
        game_handler.post(gotControlUIupdateTask);
        game_handler.post(game_logic_task);
    }

    // Game View

    private Runnable updateUITask = new Runnable() {
        @Override
        public void run() {
            updateUI();
        }
    };

    protected void updateUI() {
        // BAR
        int x = anchorView.getWidth() * (bar_p - 8) / 100; // suppose ~16% of bar witdh, so shift 8% left
        RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) bar.getLayoutParams();
        layout.setMargins(x, 0, 0, 20);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            layout.setMarginStart(x);
        bar.setLayoutParams(layout);

        // then ball or shadow...
        x = anchorView.getWidth() * x_p / 100;
        if (my_turn) {
            // BALL
            int y = anchorView.getHeight() * y_p / 100;
            layout = (RelativeLayout.LayoutParams)ball.getLayoutParams();
            layout.setMargins(x, y, 0, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                layout.setMarginStart(x);
            ball.setLayoutParams(layout);
        } else {
            // SHADOW
            layout = (RelativeLayout.LayoutParams)shadow.getLayoutParams();
            layout.setMargins(x, 10, 0, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                layout.setMarginStart(x);
            shadow.setLayoutParams(layout);
        }

        // SCORE
        if (update_score) {
            setTitle(String.format("%s [%d] x [%d] %s", you, score_mine, score_opponent, opponent));
            update_score = false;
        }
    }

    // Game Logic

    protected int bar_p = 25;
    protected int x_p;
    protected int y_p;
    protected int x_v;
    protected int y_v;
    private static int counter = 0;

    private static int abs(int v) { return v > 0 ? v : -v; }

    private Runnable game_logic_task = new Runnable() {
        @Override
        public void run() {
            // update position based on speed
            x_p += x_v;
            y_p += y_v;

            // reflection on walls
            if (x_p <= 0) { x_v = abs(x_v); }
            if (x_p >= 100) { x_v = -abs(x_v); }

            // went to other player side (transfer control)
            if (y_p <= 0) {
                my_turn = false;
                mGameController.giveControl(x_p,x_v,-y_v);
                game_handler.post(lostControlUIupdateTask);
                return;
            }

            // passed through to bottom (ball out, lose control)
            if (y_p >= 98) {
                my_turn = false;
                score_opponent++;
                update_score = true;
                mGameController.ballOut();
                game_handler.post(lostControlUIupdateTask);
                return;
            }

            // check if bar reflects the ball
            if ((y_p >= 90) && (abs(bar_p - x_p) < 8)) {
                x_v = (x_p - bar_p) / 2; // resulting horizontal velocity depends on position diff between ball and bar
                y_v = -abs(y_v);
            }

            // inform the other player about ball position (for shadow update), every 6 updates (minimize traffic)
            if (counter++ % 6 == 0)
                mGameController.updateBallPosition(x_p);

            updateUI();
            game_handler.postDelayed(this, 25); // update interval: 25ms (+ processing/IO time)
        }
    };

}