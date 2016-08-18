package projekt.substratum.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.model.SoundsInfo;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class SoundsAdapter extends RecyclerView.Adapter<SoundsAdapter.MyViewHolder> {

    private List<SoundsInfo> soundsList;

    public SoundsAdapter(List<SoundsInfo> moviesList) {
        this.soundsList = moviesList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sounds_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        SoundsInfo sounds = soundsList.get(position);
        holder.title.setText(sounds.getTitle().substring(0, sounds.getTitle().length() - 4));
    }

    @Override
    public int getItemCount() {
        return soundsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        public MyViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
        }
    }
}