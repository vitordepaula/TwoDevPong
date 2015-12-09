package br.neatfire.twodevpong.interfaces;

public interface ConnectionCallbacks {
    void onConnectionError();
    void onGameStart(String opponent_name);
}
