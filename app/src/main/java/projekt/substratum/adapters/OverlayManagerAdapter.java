package projekt.substratum.adapters;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.model.OverlayManager;

public class OverlayManagerAdapter extends
        RecyclerView.Adapter<OverlayManagerAdapter.ViewHolder> {

    private List<OverlayManager> overlayList;

    public OverlayManagerAdapter(List<OverlayManager> overlays) {
        this.overlayList = overlays;
    }

    @Override
    public OverlayManagerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.overlay_manager_row, parent, false);
        return new ViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int position) {
        final int position_fixed = position;
        viewHolder.tvName.setText(overlayList.get(position_fixed).getName());
        viewHolder.tvName.setTextColor(overlayList.get(position_fixed).getActivationValue());
        viewHolder.chkSelected.setChecked(overlayList.get(position_fixed).isSelected());
        viewHolder.chkSelected.setTag(overlayList.get(position_fixed));
        viewHolder.chkSelected.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CheckBox cb = (CheckBox) v;
                OverlayManager contact = (OverlayManager) cb.getTag();

                contact.setSelected(cb.isChecked());
                overlayList.get(position_fixed).setSelected(cb.isChecked());
            }
        });
        viewHolder.card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewHolder.chkSelected.setChecked(!viewHolder.chkSelected.isChecked());

                CheckBox cb = viewHolder.chkSelected;
                OverlayManager contact = (OverlayManager) cb.getTag();

                contact.setSelected(cb.isChecked());
                contact.setSelected(cb.isChecked());
            }
        });
    }

    @Override
    public int getItemCount() {
        return overlayList.size();
    }

    public List<OverlayManager> getOverlayManagerList() {
        return overlayList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        CheckBox chkSelected;
        CardView card;

        ViewHolder(View itemLayoutView) {
            super(itemLayoutView);
            tvName = (TextView) itemLayoutView.findViewById(R.id.tvName);
            card = (CardView) itemLayoutView.findViewById(R.id.overlayCard);
            chkSelected = (CheckBox) itemLayoutView.findViewById(R.id.chkSelected);
        }
    }
}