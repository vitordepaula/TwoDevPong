package br.neatfire.twodevpong;

import android.content.Intent;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import br.neatfire.twodevpong.util.RobolectricDataBindingTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricDataBindingTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 21)
public class GameActivityTest {

    // Test Target Class
    private GameActivity activity;
    private View anchorView;

    @Before
    public void setupTest() {
        Intent i = new Intent();
        i.putExtra("you", "Zé");
        i.putExtra("opponent", "Mané");
        activity = Robolectric.buildActivity(GameActivity.class).withIntent(i).create().start().get();
        anchorView = activity.findViewById(R.id.anchorView);
    }

    @Test
    public void checkActivityNotNull() throws Exception {
        assertNotNull(activity);
    }

    // Testing a random requirements I just invented based on code I already wrote

    // requirement: if the game screen is hidden (Activity onStop), the game shall end
    @Test
    public void testOnActivityGetsHidden() {
        // given
        GameActivity spy = Mockito.spy(activity);
        // when
        spy.onStop();
        // then
        Mockito.verify(spy, Mockito.times(1)).finish();
    }

    // Tests of RunningGameCallbacks implemented by MainActivity

    // onGameStopped()
    // game finished: the activity shall be closed
    // NOTE - although similar, this is NOT the same as onStop!
    // this callback is called when server sends a game stop event, not when activity is hidden.
    @Test
    public void testCalbackOnGameStopped() {
        // given
        GameActivity spy = Mockito.spy(activity);
        // when
        spy.onGameStopped();
        // then
        Mockito.verify(spy, Mockito.times(1)).finish();
    }

    // updateOpponentBall(int x_position)
    // the ball is with the opponent, and this gives hint of horizontal position, to update shadow on screen top
    @Test
    public void testCalbackUpdateOpponentBall() {
        // given
        GameActivity spy = Mockito.spy(activity);
        // when
        spy.updateOpponentBall(10); // 10%
        // then
        assertEquals(spy.x_p, 10);
        // TODO: the ball shadow update is posted to be handled on UI thread later. How to check if it is updated correctly?
    }

    // scoreForYou();
    // the opponent lost the ball, +1 for us
    @Test
    public void testCalbackScoreForYou() {
        // given
        GameActivity spy = Mockito.spy(activity);
        int previousScore = spy.score_mine;
        // when
        spy.scoreForYou();
        // then
        assertEquals(spy.score_mine, previousScore + 1);
    }

    // gotBallControl(int x_p, int x_v, int y_v)
    // ball crossed to our side at x_p position with (x_v,y_v) velocity vector
    @Test
    public void testCalbackGotBallControl() {
        // given
        GameActivity spy = Mockito.spy(activity);
        // when
        spy.gotBallControl(10, 20, 30);
        // then
        assertEquals(spy.x_p, 10);
        assertEquals(spy.x_v, 20);
        assertEquals(spy.y_v, 30);
        // TODO: the ball is posted to be handled on UI thread later. How to check if it is updated correctly?
    }
}