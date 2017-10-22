/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.adapters.fragments.settings;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.Packages;

public class ValidatorAdapter extends RecyclerView.Adapter<ValidatorAdapter.ViewHolder> {
    private final List<ValidatorInfo> information;

    public ValidatorAdapter(final List<ValidatorInfo> information) {
        super();
        this.information = information;
    }

    @Override
    public ValidatorAdapter.ViewHolder onCreateViewHolder(final ViewGroup viewGroup, final int i) {
        final View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.validator_dialog_entry, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int pos) {
        final ValidatorInfo validatorInfo = this.information.get(pos);
        final Context context = validatorInfo.getContext();
        String packageName = validatorInfo.getPackageName();

        if (packageName.endsWith(".common")) {
            packageName = packageName.substring(0, packageName.length() - 7);
        }

        viewHolder.packName.setText(
                String.format("%s%s",
                        Packages.getPackageName(context, packageName),
                        (validatorInfo.getCommons()) ? " " +
                                context.getString(R.string.resource_checker_commons) : ""));

        viewHolder.packIcon.setImageDrawable(
                Packages.getAppIcon(context, packageName));

        if (validatorInfo.getVerification()) {
            viewHolder.verificationIcon.setImageDrawable(
                    context.getDrawable(R.drawable.package_verification_success));
            viewHolder.verificationText.setText(
                    context.getString(R.string.resource_validated));
        } else {
            viewHolder.cardView.setOnClickListener(v -> {
                final ValidatorError error = validatorInfo.getPackageError();
                final List<String> boolErrors = error.getBoolErrors();
                final List<String> colorErrors = error.getColorErrors();
                final List<String> dimenErrors = error.getDimenErrors();
                final List<String> styleErrors = error.getStyleErrors();

                final StringBuilder error_logs = new StringBuilder();
                if (boolErrors.size() > 0) {
                    for (int i = 0; i < boolErrors.size(); i++) {
                        error_logs.append(boolErrors.get(i)).append("\n");
                    }
                }
                if (colorErrors.size() > 0) {
                    for (int i = 0; i < colorErrors.size(); i++) {
                        error_logs.append(colorErrors.get(i)).append("\n");
                    }
                }
                if (dimenErrors.size() > 0) {
                    for (int i = 0; i < dimenErrors.size(); i++) {
                        error_logs.append(dimenErrors.get(i)).append("\n");
                    }
                }
                if (styleErrors.size() > 0) {
                    for (int i = 0; i < styleErrors.size(); i++) {
                        error_logs.append(styleErrors.get(i)).append("\n");
                    }
                }

                String pkg = validatorInfo.getPackageName();
                switch (validatorInfo.getPackageName()) {
                    case "com.android.contacts.common":
                        pkg = "com.android.contacts";
                        break;
                    case "com.android.phone.common":
                        pkg = "com.android.phone";
                        break;
                }
                final String format = String.format(
                        context.getString(R.string.resource_commit_dialog_title),
                        Packages.getPackageName(context, pkg));
                new android.app.AlertDialog.Builder(context)
                        .setTitle(format)
                        .setMessage("\n" + error_logs)
                        .setPositiveButton(R.string
                                .customactivityoncrash_error_activity_error_details_close, null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return this.information.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView packName;
        TextView verificationText;
        ImageView packIcon;
        ImageView verificationIcon;

        ViewHolder(final View view) {
            super(view);
            this.cardView = view.findViewById(R.id.pack_card);
            this.packIcon = view.findViewById(R.id.pack_icon);
            this.verificationIcon = view.findViewById(R.id.verification);
            this.packName = view.findViewById(R.id.pack_name);
            this.verificationText = view.findViewById(R.id.verification_text);
        }
    }
}