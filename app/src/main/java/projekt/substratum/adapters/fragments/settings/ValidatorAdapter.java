/*
 * Copyright (c) 2016-2018 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */

package projekt.substratum.adapters.fragments.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import projekt.substratum.R;
import projekt.substratum.common.Packages;
import projekt.substratum.databinding.ValidatorDialogEntryBinding;

import java.util.List;

import static projekt.substratum.common.Internal.CONTACTS;
import static projekt.substratum.common.Internal.CONTACTS_COMMON_FRAMEWORK;
import static projekt.substratum.common.Internal.PHONE;
import static projekt.substratum.common.Internal.PHONE_COMMON_FRAMEWORK;

public class ValidatorAdapter extends RecyclerView.Adapter<ValidatorAdapter.ViewHolder> {
    private final List<ValidatorInfo> information;

    public ValidatorAdapter(List<ValidatorInfo> information) {
        super();
        this.information = information;
    }

    @NonNull
    @Override
    public ValidatorAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                          int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.validator_dialog_entry, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder,
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
                    for (String boolError : boolErrors) {
                        errorLogs.append(boolError).append('\n');
                    }
                }
                if (!colorErrors.isEmpty()) {
                    for (String colorError : colorErrors) {
                        errorLogs.append(colorError).append('\n');
                    }
                }
                if (!dimenErrors.isEmpty()) {
                    for (String dimenError : dimenErrors) {
                        errorLogs.append(dimenError).append('\n');
                    }
                }
                if (!styleErrors.isEmpty()) {
                    for (String styleError : styleErrors) {
                        errorLogs.append(styleError).append('\n');
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
        final ValidatorDialogEntryBinding binding;

        ViewHolder(View view) {
            super(view);
            binding = DataBindingUtil.bind(view);
        }

        ValidatorDialogEntryBinding getBinding() {
            return binding;
        }
    }
}