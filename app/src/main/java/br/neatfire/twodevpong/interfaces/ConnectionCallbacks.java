package br.neatfire.twodevpong.interfaces;

public interface ConnectionCallbacks {
    void onConnectionError();
    void onGameStart(String user_id);
}
