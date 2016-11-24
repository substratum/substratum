package projekt.substratum.model;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import projekt.substratum.R;

public class IconEntry extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView iconName;
    public ImageView iconDrawable;

    public IconEntry(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        iconName = (TextView) itemView.findViewById(R.id.icon_pack_package);
        iconDrawable = (ImageView) itemView.findViewById(R.id.icon_pack_icon);
    }

    @Override
    public void onClick(View view) {
        // Temporarily don't have a response
    }
}