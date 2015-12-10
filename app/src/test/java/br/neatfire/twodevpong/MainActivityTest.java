package br.neatfire.twodevpong;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowToast;

import br.neatfire.twodevpong.util.RobolectricDataBindingTestRunner;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricDataBindingTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = 21)
public class MainActivityTest {

    // Test Target Class
    private MainActivity activity;
    private TextView tvUserName;
    private Button btnServeGame;

    @Before
    public void setupTest() {
        activity = Robolectric.setupActivity(MainActivity.class);
        tvUserName = (TextView)activity.findViewById(R.id.tvName);
        btnServeGame = (Button)activity.findViewById(R.id.btn_serve);
    }

    @Test
    public void checkActivityNotNull() throws Exception {
        assertNotNull(activity);
    }

    // Testing a few random requirements I just invented based on code I already wrote

    // requirement: user shall always have a name (not null or empty) after onCreate
    // requirement: name must appear on screen, on the TextView
    @Test
    public void checkUserHasNameAssigned() throws Exception {
        // given
        MainActivity spy = Mockito.spy(activity);
        // when
        //spy.onCreate(null); -- onCreate is already called when activity is setup
        // expect
        assertNotNull(spy.my_name);
        assertTrue(spy.my_name.length() > 0);
        assertTrue(tvUserName.getText().toString().startsWith(spy.my_name));
    }

    // requirement: upon click of serve button, an AlertDialog shall open to wait for new game
    @Test
    public void checkOnServeButton() {
        // when
        btnServeGame.performClick();
        // then
        AlertDialog waitingDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(waitingDialog.isShowing());
        // and
        TextView tvMessage = (TextView)waitingDialog.findViewById(android.R.id.message);
        assertEquals(tvMessage.getText().toString(), activity.getString(R.string.waiting_connection));
    }

    // requirement: a long press on name shall open an AlertDialog for user to enter a new name
    @Test
    public void checkOnNameLongPress() {
        // when
        tvUserName.performLongClick();
        // then
        AlertDialog newNameDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(newNameDialog.isShowing());
        // and
        int titleId = activity.getResources().getIdentifier("alertTitle", "id", "android");
        TextView dialogTitle = (TextView) newNameDialog.findViewById(titleId);
        assertEquals(dialogTitle.getText().toString(),activity.getString(R.string.enter_your_name));
    }

    // Tests of interface ConnectionCallbacks implemented by MainActivity

    // ConnectionCallbacks.onGameStart
    @Test
    public void checkOnGameStart() {
        // given
        MainActivity spy = Mockito.spy(activity);
        // when
        spy.onGameStart("Opponent Name");
        // expect
        Intent intent = Shadows.shadowOf(spy).peekNextStartedActivity();
        assertEquals(GameActivity.class.getCanonicalName(), intent.getComponent().getClassName());
    }

    // ConnectionCallbacks.onConnectionError
    @Test
    public void checkOnConnectionError() {
        // given
        MainActivity spy = Mockito.spy(activity);
        // when
        spy.onConnectionError();
        // then
        assertThat(ShadowToast.getTextOfLatestToast(), equalTo(spy.getString(R.string.connection_error)));
        // and
        Mockito.verify(spy, Mockito.times(1)).finish();
    }
}
