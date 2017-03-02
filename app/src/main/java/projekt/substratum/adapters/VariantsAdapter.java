package projekt.substratum.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.model.VariantInfo;

public class VariantsAdapter extends ArrayAdapter<VariantInfo> {

    private final ArrayList<VariantInfo> variants;

    public VariantsAdapter(Context context, ArrayList<VariantInfo> variants) {
        super(context, R.layout.preview_spinner, R.id.variant_name, variants);
        this.variants = variants;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.preview_spinner, parent, false);

            holder = new ViewHolder();
            holder.variantName = (TextView) convertView.findViewById(R.id.variant_name);
            holder.variantHex = (ImageView) convertView.findViewById(R.id.variant_hex);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        VariantInfo item = getItem(position);
        if (item != null) {
            holder.variantName.setText(item.getVariantName());
            int color = Color.parseColor(item.getVariantHex());
            ColorStateList csl = new ColorStateList(
                    new int[][]{
                            new int[]{android.R.attr.state_checked},
                            new int[]{}
                    },
                    new int[]{
                            color,
                            color
                    }
            );
            holder.variantHex.setImageTintList(csl);
        }

        return convertView;
    }

    public class ViewHolder {
        TextView variantName;
        ImageView variantHex;
    }
}