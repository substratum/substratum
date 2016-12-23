package projekt.substratum.model;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import projekt.substratum.R;

public class IconEntry extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView iconName;
    public ImageView iconDrawable;
    public Context mContext;
    private Boolean willBeModified = false;

    public IconEntry(Context mContext, View itemView) {
        super(itemView);
        this.mContext = mContext;
        itemView.setOnClickListener(this);
        iconName = (TextView) itemView.findViewById(R.id.icon_pack_package);
        iconDrawable = (ImageView) itemView.findViewById(R.id.icon_pack_icon);
    }

    public void setDisabled(Boolean bool) {
        this.willBeModified = bool;
    }

    @Override
    public void onClick(View view) {
        if (willBeModified) {
            Snackbar.make(view,
                    mContext.getString(R.string.studio_toast_mask),
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }
}