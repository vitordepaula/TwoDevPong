package br.neatfire.twodevpong.interfaces;

public interface RunningGameCallbacks {

    // game finished
    void onGameStopped();

    // the ball is with the opponent, and this gives hint of horizontal position
    void updateOpponentBall(int x_p);

    // the opponent lost the ball, +1 for us
    void scoreForYou();

    // ball is now on our side
    // at x_p % screen horizontal position,
    // with x_v % change of horizontal velocity per update interval and
    // with y_v % change of vertical velocity per update interval
    void gotBallControl(int x_p, int x_v, int y_v);
}
