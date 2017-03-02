package projekt.substratum.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.model.VariantInfo;

public class VariantsAdapter extends ArrayAdapter<VariantInfo> {

    private final ArrayList<VariantInfo> variants;

    public class ViewHolder {
        TextView variantName;
        ImageView variantHex;
    }

    public VariantsAdapter(Context context, ArrayList<VariantInfo> variants) {
        super(context, R.layout.spinner_row, R.id.variant_name, variants);

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

    private View getCustomView(int position, View convertView, ViewGroup parent){
        ViewHolder holder = null;

        if(convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.spinner_row, parent, false);

            holder = new ViewHolder();
            holder.variantName = (TextView) convertView.findViewById(R.id.variant_name);
            holder.variantHex = (ImageView) convertView.findViewById(R.id.variant_hex);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        VariantInfo item = getItem(position);
        if (item!= null) {
            holder.variantName.setText(item.getVariantName());
            holder.variantHex.setBackgroundColor(Color.parseColor(item.getVariantHex()));
        }

        return convertView;
    }
}