package br.neatfire.twodevpong.model;

public class Game {

    private String mId;
    private String mUserName;

    public Game(String id, String username) {
        mId = id;
        mUserName = username;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getId() {
        return mId;
    }
}
