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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;

import java.util.ArrayList;

import projekt.substratum.R;
import projekt.substratum.common.References;

public class ValidatorAdapter extends RecyclerView.Adapter<ValidatorAdapter.ViewHolder> {
    private ArrayList<ValidatorInfo> information;

    public ValidatorAdapter(ArrayList<ValidatorInfo> information) {
        this.information = information;
    }

    @Override
    public ValidatorAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.validator_dialog_entry, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int pos) {
        ValidatorInfo validatorInfo = information.get(pos);
        Context context = validatorInfo.getContext();
        String packageName = validatorInfo.getPackageName();

        if (packageName.endsWith(".common")) {
            packageName = packageName.substring(0, packageName.length() - 7);
        }

        viewHolder.packName.setText(
                String.format("%s%s",
                        References.grabPackageName(context, packageName),
                        (validatorInfo.getCommons()) ? " " +
                                context.getString(R.string.resource_checker_commons) : ""));

        viewHolder.packIcon.setImageDrawable(
                References.grabAppIcon(context, packageName));

        if (validatorInfo.getVerification()) {
            viewHolder.numberProgressBar.setProgressTextColor(
                    context.getColor(R.color.number_progress_bar_validation_success));
            viewHolder.numberProgressBar.setReachedBarColor(
                    context.getColor(R.color.number_progress_bar_validation_success));
            viewHolder.verificationIcon.setImageDrawable(
                    context.getDrawable(R.drawable.package_verification_success));
            viewHolder.verificationText.setText(
                    context.getString(R.string.resource_validated));
            viewHolder.numberProgressBar.setProgress(100);
        } else {
            viewHolder.cardView.setOnClickListener(v -> {
                ValidatorError error = validatorInfo.getPackageError();
                ArrayList<String> boolErrors = error.getBoolErrors();
                ArrayList<String> colorErrors = error.getColorErrors();
                ArrayList<String> dimenErrors = error.getDimenErrors();
                ArrayList<String> styleErrors = error.getStyleErrors();

                StringBuilder error_logs = new StringBuilder();
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

                final Dialog dialog = new Dialog(context,
                        android.R.style.Theme_DeviceDefault_Dialog);
                dialog.setContentView(R.layout.commit_dialog);
                String format = String.format(
                        context.getString(R.string.resource_commit_dialog_title),
                        viewHolder.packName.getText());
                dialog.setTitle(format);
                if (dialog.getWindow() != null)
                    dialog.getWindow().setLayout(RecyclerView.LayoutParams.MATCH_PARENT,
                            RecyclerView.LayoutParams.WRAP_CONTENT);

                TextView text = (TextView) dialog.findViewById(R.id.textField);
                text.setText(error_logs.toString());
                ImageButton confirm = (ImageButton) dialog.findViewById(R.id.confirm);
                confirm.setOnClickListener(view -> dialog.dismiss());

                ImageButton send = (ImageButton) dialog.findViewById(
                        R.id.send);
                send.setOnClickListener(v2 -> {
                    String device = " " + Build.MODEL + " (" + Build.DEVICE + ") " +
                            "[" + Build.FINGERPRINT + "]";
                    String email_subject = context.getString(
                            R.string.resource_commit_dialog_subject);
                    String xposed = References.checkXposedVersion();
                    if (xposed.length() > 0) {
                        device += " {" + xposed + "}";
                    }
                    String email_body =
                            String.format(context.getString(
                                    R.string.resource_commit_dialog_body), device, error_logs,
                                    References.getBuildProp().toString());
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{
                            context.getString(R.string.themer_email)});
                    i.putExtra(Intent.EXTRA_SUBJECT, email_subject);
                    i.putExtra(Intent.EXTRA_TEXT, email_body);
                    try {
                        context.startActivity(Intent.createChooser(i, context.getString(
                                R.string.resource_checker_request)));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(context,
                                context.getString(R.string.logcat_email_activity_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });

                dialog.show();
            });

            viewHolder.numberProgressBar.setProgressTextColor(
                    context.getColor(R.color.number_progress_bar_validation_error));
            viewHolder.numberProgressBar.setReachedBarColor(
                    context.getColor(R.color.number_progress_bar_validation_error));
            viewHolder.numberProgressBar.setProgress(validatorInfo.getPercentage());
        }
    }

    @Override
    public int getItemCount() {
        return information.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView packName;
        TextView verificationText;
        ImageView packIcon;
        ImageView verificationIcon;
        NumberProgressBar numberProgressBar;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view.findViewById(R.id.pack_card);
            packIcon = (ImageView) view.findViewById(R.id.pack_icon);
            verificationIcon = (ImageView) view.findViewById(R.id.verification);
            packName = (TextView) view.findViewById(R.id.pack_name);
            verificationText = (TextView) view.findViewById(R.id.verification_text);
            numberProgressBar = (NumberProgressBar) view.findViewById(R.id.number_progress_bar);
        }
    }
}