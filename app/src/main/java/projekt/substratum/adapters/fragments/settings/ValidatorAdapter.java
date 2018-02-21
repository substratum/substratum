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
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.databinding.ValidatorDialogEntryBinding;

import static projekt.substratum.common.Internal.CONTACTS;
import static projekt.substratum.common.Internal.CONTACTS_COMMON_FRAMEWORK;
import static projekt.substratum.common.Internal.PHONE;
import static projekt.substratum.common.Internal.PHONE_COMMON_FRAMEWORK;

public class ValidatorAdapter extends RecyclerView.Adapter<ValidatorAdapter.ViewHolder> {
    private List<ValidatorInfo> information;

    public ValidatorAdapter(List<ValidatorInfo> information) {
        super();
        this.information = information;
    }

    @Override
    public ValidatorAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup,
                                                          int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.validator_dialog_entry, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder,
                                 int pos) {
        ValidatorInfo validatorInfo = this.information.get(pos);
        Context context = validatorInfo.getContext();
        String packageName = validatorInfo.getPackageName();
        ValidatorDialogEntryBinding viewHolderBinding = viewHolder.getBinding();

        if (packageName.endsWith(".common")) {
            packageName = packageName.substring(0, packageName.length() - 7);
        }

        viewHolderBinding.packName.setText(
                String.format("%s%s",
                        Packages.getPackageName(context, packageName),
                        (validatorInfo.getCommons()) ? (' ' +
                                context.getString(R.string.resource_checker_commons)) : ""));


        viewHolderBinding.packIcon.setImageDrawable(
                Packages.getAppIcon(context, packageName));

        if (validatorInfo.getVerification()) {
            viewHolderBinding.verificationIcon.setImageDrawable(
                    context.getDrawable(R.drawable.package_verification_success));
            viewHolderBinding.verificationText.setText(
                    context.getString(R.string.resource_validated));
        } else {
            viewHolderBinding.packCard.setOnClickListener(v -> {
                ValidatorError error = validatorInfo.getPackageError();
                List<String> boolErrors = error.getBoolErrors();
                List<String> colorErrors = error.getColorErrors();
                List<String> dimenErrors = error.getDimenErrors();
                List<String> styleErrors = error.getStyleErrors();

                StringBuilder errorLogs = new StringBuilder();
                if (!boolErrors.isEmpty()) {
                    for (int i = 0; i < boolErrors.size(); i++) {
                        errorLogs.append(boolErrors.get(i)).append('\n');
                    }
                }
                if (!colorErrors.isEmpty()) {
                    for (int i = 0; i < colorErrors.size(); i++) {
                        errorLogs.append(colorErrors.get(i)).append('\n');
                    }
                }
                if (!dimenErrors.isEmpty()) {
                    for (int i = 0; i < dimenErrors.size(); i++) {
                        errorLogs.append(dimenErrors.get(i)).append('\n');
                    }
                }
                if (!styleErrors.isEmpty()) {
                    for (int i = 0; i < styleErrors.size(); i++) {
                        errorLogs.append(styleErrors.get(i)).append('\n');
                    }
                }

                String pkg = validatorInfo.getPackageName();
                switch (validatorInfo.getPackageName()) {
                    case CONTACTS_COMMON_FRAMEWORK:
                        pkg = CONTACTS;
                        break;
                    case PHONE_COMMON_FRAMEWORK:
                        pkg = PHONE;
                        break;
                }
                String format = String.format(
                        context.getString(R.string.resource_commit_dialog_title),
                        Packages.getPackageName(context, pkg));
                new android.app.AlertDialog.Builder(context)
                        .setTitle(format)
                        .setMessage("\n" + errorLogs)
                        .setPositiveButton(R.string
                                .customactivityoncrash_error_activity_error_details_close, null)
                        .show();
            });
        }
        viewHolderBinding.setValidatorInfo(validatorInfo);
        viewHolderBinding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return this.information.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ValidatorDialogEntryBinding binding;

        ViewHolder(View view) {
            super(view);
            binding = DataBindingUtil.bind(view);
        }

        ValidatorDialogEntryBinding getBinding() {
            return binding;
        }
    }
}