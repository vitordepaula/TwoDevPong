package br.neatfire.twodevpong;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import br.neatfire.twodevpong.interfaces.ConnectionCallbacks;
import br.neatfire.twodevpong.model.GameController;

public class MainActivity extends AppCompatActivity implements ConnectionCallbacks {

    private GameController mGameController;
    private AlertDialog mWaitingConnectionDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ((Button)findViewById(R.id.btn_serve)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGameController.openGame();
                mWaitingConnectionDialog = new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getString(R.string.waiting_connection))
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mGameController.closeGame();
                            }
                        }).show();
                mWaitingConnectionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mGameController.closeGame();
                    }
                });
            }
        });

        mGameController = GameController.getInstance(this,this);

        RecyclerView mRecList = ((RecyclerView) findViewById(R.id.game_list));
        mRecList.setAdapter(mGameController.getAdapter());

        mRecList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecList.setLayoutManager(llm);
        mRecList.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mGameController.joinGame(position);
            }
        }));

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
        } catch (Exception e){
            Log.e("chat application", e.getMessage());
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGameController.onFinish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGameController.getAdapter().notifyDataSetChanged(); // refresh RecyclerView
    }

    @Override
    public void onConnectionError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, R.string.connection_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onGameStart(String user_id) {
        if (mWaitingConnectionDialog != null) {
            mWaitingConnectionDialog.dismiss();
            mWaitingConnectionDialog = null;
        }
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("user_id", user_id);
        startActivity(i);
    }
}
