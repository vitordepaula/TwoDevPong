package br.neatfire.twodevpong.model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import br.neatfire.twodevpong.R;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private List<Game> mGameList;
    //private String loggedUser;
    private int lastPosition = -1;
    private Context context;
    private Handler UIHandler;

    public GameAdapter(Context context) {
        this.mGameList = new ArrayList<>();
        //this.loggedUser = loggedUser;
        this.context = context;
        this.UIHandler = new Handler(Looper.getMainLooper());
    }

    private Runnable UpdateTask = new Runnable() {
        @Override
        public void run() {
            notifyDataSetChanged();
        }
    };

    @Override
    public int getItemCount() {
        return mGameList.size();
    }

    public void addGame(String user_id, String user_name) {
        mGameList.add(new Game(user_id, user_name));
        UIHandler.post(UpdateTask);
    }

    public void removeGame(String user_id) {
        Iterator<Game> itr = mGameList.iterator();
        Game g, game = null;
        for(;itr.hasNext();) {
            g = itr.next();
            if (g.getId().equals(user_id)) {
                game = g;
            }
        }
        if (game != null) {
            mGameList.remove(game);
            UIHandler.post(UpdateTask);
        }
    }

    public String getGameId(int list_position) {
        if (list_position < 0 || list_position >= mGameList.size())
            return null;
        return mGameList.get(list_position).getId();
    }

    @Override
    public void onBindViewHolder(final GameViewHolder holder, int position) {
        Game item = mGameList.get(position);
        holder.tvName.setText(item.getUserName());
        setAnimation(holder.cardView, position);
    }

    @Override
    public GameViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.game_card, viewGroup, false);
        return new GameViewHolder(itemView);
    }

    private void setAnimation(CardView cardView, int position) {
        if (position >= lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
            cardView.startAnimation(animation);
            lastPosition = position;
        }
    }

    public static class GameViewHolder extends RecyclerView.ViewHolder {
        protected CardView cardView;
        protected TextView tvName;

        public GameViewHolder(View v) {
            super(v);
            cardView = (CardView) v.findViewById(R.id.card_view);
            tvName = (TextView) v.findViewById(R.id.tvName);
        }
    }
}
