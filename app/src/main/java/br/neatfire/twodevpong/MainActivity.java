package br.neatfire.twodevpong;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import br.neatfire.twodevpong.interfaces.ConnectionCallbacks;
import br.neatfire.twodevpong.model.GameController;

public class MainActivity extends AppCompatActivity implements ConnectionCallbacks {

    private GameController mGameController;
    private AlertDialog mWaitingConnectionDialog = null;

    private static final String PREFS_NAME = "MyPrefsFile";
    private String my_name;
    private TextView tvName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        my_name = settings.getString("username", "");
        if (my_name.isEmpty()) my_name = storeNewName(null); // null will generate a new random name

        tvName = ((TextView)findViewById(R.id.tvName));
        tvName.setText(my_name + ',');
        tvName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.enter_your_name);
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (input.getText().length() > 0 && input.getText().length() < 45) {
                            my_name = input.getText().toString();
                            storeNewName(my_name);
                            tvName.setText(my_name + ',');
                            mGameController.setName(my_name);
                        }
                    }
                });
                builder.setNeutralButton(R.string.random, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        my_name = storeNewName(null);
                        tvName.setText(my_name + ',');
                        mGameController.setName(my_name);
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
                return true;
            }
        });

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
        mGameController.setName(my_name);

        RecyclerView mRecList = ((RecyclerView) findViewById(R.id.game_list));
        mRecList.setAdapter(mGameController.getAdapter());

        mRecList.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecList.setLayoutManager(llm);
        mRecList.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mGameController.joinGame(position, my_name);
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

    private String storeNewName(String name) {
        if (name == null) {
            // If null supplied, generate a random name
            final String[] first = {"Jonatan", "William", "Guilherme", "João", "Pedro",
                    "Marcos", "Mário", "Plínio", "Felipe", "Joaquim", "Bráulio", "Lindomar",
                    "Júlio", "Vitor", "Heitor", "Washington", "Flávio", "Hugo"};
            final String[] second = {"Cardoso", "Oliveira", "Santana", "Pereira", "Mendes",
                    "Dias", "D'Avila", "Carvalho", "dos Santos", "de Paula", "Vicente", "Pedroso",
                    "de Morais", "Andrade"};
            Random r = new Random();
            StringBuilder b = new StringBuilder(first[r.nextInt(first.length)])
                    .append(' ').append(second[r.nextInt(second.length)]);
            if (r.nextInt(2) == 0)
                b.append(' ').append(second[r.nextInt(second.length)]); // 50% chance of 3rd name
            name = b.toString();
        }
        // Store preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor edt = settings.edit();
        edt.putString("username", name);
        edt.commit();
        return name;
    };

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
    public void onGameStart(String opponent_name) {
        if (mWaitingConnectionDialog != null) {
            mWaitingConnectionDialog.dismiss();
            mWaitingConnectionDialog = null;
        }
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra("you", my_name);
        i.putExtra("opponent", opponent_name);
        startActivity(i);
    }
}
