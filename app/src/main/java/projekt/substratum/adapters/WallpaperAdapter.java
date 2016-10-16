package projekt.substratum.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.model.WallpaperEntries;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {
    private ArrayList<WallpaperEntries> information;

    public WallpaperAdapter(ArrayList<WallpaperEntries> information) {
        this.information = information;
    }

    @Override
    public WallpaperAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout
                        .wallpaper_entry_card,
                viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int i) {
        Glide.with(information.get(i).getContext())
                .load(information.get(i).getWallpaperPreview())
                .centerCrop()
                .crossFade()
                .into(viewHolder.imageView);

        viewHolder.wallpaperName.setText(information.get(i).getWallpaperName());

        viewHolder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("Link to launch", information.get(i).getWallpaperLink());
            }
        });
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView wallpaperName;
        ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.wallpaperCard);
            wallpaperName = (TextView) view.findViewById(R.id.wallpaperName);
            imageView = (ImageView) view.findViewById(R.id.wallpaperImage);

        }
    }
}