package br.neatfire.twodevpong.model;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import br.neatfire.twodevpong.interfaces.ConnectionCallbacks;
import br.neatfire.twodevpong.interfaces.RunningGameCallbacks;

public class GameController {

    private static final String TAG = GameController.class.getSimpleName();
    private static GameController mInstance = null;

    public static GameController getInstance(Context ctx, ConnectionCallbacks cbs) {
        if (mInstance == null) {
            mInstance = new GameController();
            mInstance.mContext = ctx;
            mInstance.mGameRunningCallbacks = null;
        }
        if (mInstance.mCallbacks == null)
            mInstance.mCallbacks = cbs;
        if (mInstance.mSocket == null)
            mInstance.onInitialize();
        return mInstance;
    }

    private GameAdapter mGameAdapter;
    private Context mContext;
    private ConnectionCallbacks mCallbacks;
    private RunningGameCallbacks mGameRunningCallbacks;
    private Socket mSocket;
    private UUID myId;
    private String myName;

    private void onInitialize() {
        try {
            mSocket = IO.socket("http://vps.de.paula.nom.br:7664" /*199.193.253.49:7664(pong)*/);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
            mSocket.on(Socket.EVENT_CONNECT, onConnect);
            mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            mSocket.on("game opened", onGameOpened);
            mSocket.on("game closed", onGameClosed);
            mSocket.on("game started", onGameStart);
            mSocket.on("game stopped", onGameStop);
            mGameAdapter = new GameAdapter(mContext);

            myId = UUID.randomUUID();
            myName = myId.toString(); // until initialized by setName, will hold the ID

            Log.d(TAG, "socket initialized");
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            mCallbacks.onConnectionError();
            mInstance = null;
        }
    }

    public void onFinish() {
        mSocket.disconnect();
        mCallbacks = null;
        mInstance = null;
    }

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Disconnected");
            mSocket.off(); // remove all listeners
            mSocket = null;
        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "Connected");
            mSocket.emit("login", myId);
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mCallbacks.onConnectionError();
        }
    };

    private Emitter.Listener onGameOpened = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String id, username;
            try {
                id = data.getString("user_id");
                username = data.getString("user_name");
                mGameAdapter.addGame(id, username);
                Log.d(TAG, "Serving game " + id);
                playSound();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener onGameClosed = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String id;
            try {
                id = data.getString("user_id");
                mGameAdapter.removeGame(id);
                Log.d(TAG, "Game removed " + id);
                playSound();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void playSound() {
        try {
            AssetFileDescriptor afd = mContext.getAssets().openFd("FacebookSound.mp3");
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Emitter.Listener onGameStart = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String opponent_name;
            try {
                opponent_name = data.getString("user_name"); // Name of the other gamer
                mCallbacks.onGameStart(opponent_name);

                mSocket.on("ball update", onShadowUpdate);
                mSocket.on("give control", onGetControl);
                mSocket.on("score", onScore);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener onGameStop = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mSocket.off("ball update");
            mSocket.off("give control");
            mSocket.off("score");
            if (mGameRunningCallbacks != null) {
                mGameRunningCallbacks.onGameStopped();
                mGameRunningCallbacks = null;
            }
        }
    };

    private Emitter.Listener onShadowUpdate = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            int x_p;
            try {
                x_p = data.getInt("x_p");
                if (mGameRunningCallbacks != null)
                    mGameRunningCallbacks.updateOpponentBall(x_p);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener onGetControl = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            int x_p, x_v, y_v;
            try {
                x_p = data.getInt("x_p");
                x_v = data.getInt("x_v");
                y_v = data.getInt("y_v");
                if (mGameRunningCallbacks != null)
                    mGameRunningCallbacks.gotBallControl(x_p, x_v, y_v);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener onScore = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (mGameRunningCallbacks != null)
                mGameRunningCallbacks.scoreForYou();
        }
    };

    public GameAdapter getAdapter() {
        return mGameAdapter;
    }

    public void openGame() {
        mSocket.emit("open game", myName);
    }

    public void closeGame() {
        mSocket.emit("close game");
    }

    public void joinGame(int list_position, String my_name) {
        String other_user_id = mGameAdapter.getGameId(list_position);
        mGameAdapter.removeGame(other_user_id);
        mSocket.emit("join game", other_user_id, my_name);
    }

    public void registerRunningGameCallbacks(RunningGameCallbacks rgcb) {
        mGameRunningCallbacks = rgcb;
    }

    // All call related to running game (all below) shall test for null socket,
    // as the game can be suddenly disconnected at any time by network error or
    // by other side

    public void leaveGame() {
        if (mSocket != null) // may be null if game being stopped due to an abnormal disconnection
            mSocket.emit("leave game");
        mGameRunningCallbacks = null;
    }

    // tell server we're ready to play
    public void im_ready() {
        if (mSocket != null)
            mSocket.emit("ready");
    }

    // tell other side where ball (horizontal tip only, 0-100% of screen witdh)
    public void updateBallPosition(int x_p) {
        if (mSocket != null)
            mSocket.emit("ball update", x_p);
    }

    /**
     *     ball goes to other user device
     *     x_p: ball position in percent of screen witdh (0 - 100)
     *     x_v: the horizontal velocity of the ball (% per update interval)
     *     y_v: the vertical velocity of the ball (% per update interval)
     */
    public void giveControl(int x_p, int x_v, int y_v) {
        if (mSocket != null)
            mSocket.emit("give control", x_p, x_v, y_v);
    }

    // ball out! Score to other side
    public void ballOut() {
        if (mSocket != null)
            mSocket.emit("ball out");
    }

    public void setName(String name) {
        this.myName = name;
    }
}
