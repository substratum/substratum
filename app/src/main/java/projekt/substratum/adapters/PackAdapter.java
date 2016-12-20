package projekt.substratum.adapters;

import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.StudioPreviewActivity;
import projekt.substratum.config.References;
import projekt.substratum.model.PackInfo;

public class PackAdapter extends RecyclerView.Adapter<PackAdapter.ViewHolder> {
    private ArrayList<PackInfo> information;

    public PackAdapter(ArrayList<PackInfo> information) {
        this.information = information;
    }

    @Override
    public PackAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout
                .icon_studio_pack_entry, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int pos) {
        final int i = pos;

        viewHolder.cardView.setOnClickListener(
                v -> {
                    Intent intent = new Intent(information.get(i).getContext(),
                            StudioPreviewActivity.class);
                    intent.putExtra("icon_pack", information.get(i).getPackageName());
                    information.get(i).getContext().startActivity(intent);
                });

        viewHolder.packName.setText(References.grabPackageName(information.get(i).getContext(),
                information.get(i).getPackageName()));

        viewHolder.packIcon.setImageDrawable(References.grabAppIcon(information.get(i).getContext(),
                information.get(i).getPackageName()));
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView packName;
        ImageView packIcon;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.pack_card);
            packIcon = (ImageView) view.findViewById(R.id.pack_icon);
            packName = (TextView) view.findViewById(R.id.pack_name);
        }
    }
}