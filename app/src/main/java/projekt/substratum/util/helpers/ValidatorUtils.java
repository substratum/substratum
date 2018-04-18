package projekt.substratum.util.helpers;

import android.app.Dialog;
import android.os.AsyncTask;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.adapters.fragments.settings.Repository;
import projekt.substratum.adapters.fragments.settings.ValidatorAdapter;
import projekt.substratum.adapters.fragments.settings.ValidatorError;
import projekt.substratum.adapters.fragments.settings.ValidatorFilter;
import projekt.substratum.adapters.fragments.settings.ValidatorInfo;
import projekt.substratum.common.Packages;
import projekt.substratum.common.References;
import projekt.substratum.common.Systems;
import projekt.substratum.fragments.SettingsFragment;
import projekt.substratum.util.readers.ReadFilterFile;
import projekt.substratum.util.readers.ReadRepositoriesFile;
import projekt.substratum.util.readers.ReadResourcesFile;

import static projekt.substratum.common.Internal.VALIDATOR_CACHE;
import static projekt.substratum.common.Internal.VALIDATOR_CACHE_DIR;
import static projekt.substratum.common.References.SUBSTRATUM_VALIDATOR;
import static projekt.substratum.common.References.VALIDATE_WITH_LOGS;

public class ValidatorUtils {

    /**
     * Check if the ROM list on our GitHub organization lists the current device as a fully
     * supported, community based ROM.
     */
    public static class checkROMSupportList extends AsyncTask<String, Integer, String> {

        private WeakReference<SettingsFragment> ref;

        public checkROMSupportList(SettingsFragment settingsFragment) {
            super();
            ref = new WeakReference<>(settingsFragment);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment == null) return;
            try {
                if (!Systems.checkThemeInterfacer(settingsFragment.context) &&
                        !Systems.checkSubstratumService(settingsFragment.context)) {
                    return;
                }

                if (!References.isNetworkAvailable(settingsFragment.context)) {
                    settingsFragment.platformSummary.append('\n')
                            .append(settingsFragment.getString(R.string.rom_status))
                            .append(' ')
                            .append(settingsFragment.getString(R.string.rom_status_network));
                    settingsFragment.systemPlatform.setSummary(
                            settingsFragment.platformSummary.toString());
                    return;
                }

                if (!result.isEmpty()) {
                    String supportedRom = String.format(
                            settingsFragment.getString(R.string.rom_status_supported), result);
                    settingsFragment.platformSummary.append('\n')
                            .append(settingsFragment.getString(R.string.rom_status))
                            .append(' ')
                            .append(supportedRom);
                    settingsFragment.systemPlatform.setSummary(
                            settingsFragment.platformSummary.toString());
                    return;
                }

                settingsFragment.platformSummary.append('\n')
                        .append(settingsFragment.getString(R.string.rom_status))
                        .append(' ')
                        .append(settingsFragment.getString(R.string.rom_status_unsupported));
                settingsFragment.systemPlatform.setSummary(
                        settingsFragment.platformSummary.toString());
            } catch (IllegalStateException ignored) { /* Not much we can do about this */}
        }

        @Override
        protected String doInBackground(String... sUrl) {
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                return Systems.checkFirmwareSupport(settingsFragment.context, sUrl[0], sUrl[1]);
            }
            return null;
        }
    }

    /**
     * Class that downloads the upstreamed repositories from our GitHub organization, to tell the
     * user whether the device has any missing commits (Validator)
     */
    public static class downloadRepositoryList extends AsyncTask<String, Integer,
            ArrayList<String>> {

        private WeakReference<SettingsFragment> ref;

        public downloadRepositoryList(SettingsFragment settingsFragment) {
            super();
            ref = new WeakReference<>(settingsFragment);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                if (settingsFragment.getActivity() != null) {
                    settingsFragment.dialog = new Dialog(settingsFragment.getActivity());
                    settingsFragment.dialog.setContentView(R.layout.validator_dialog);
                    settingsFragment.dialog.setCancelable(false);
                    settingsFragment.dialog.show();
                }
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            super.onPostExecute(result);
            SettingsFragment settingsFragment = ref.get();
            if (settingsFragment != null) {
                Collection<String> erroredPackages = new ArrayList<>();
                for (int x = 0; x < settingsFragment.errors.size(); x++) {
                    ValidatorError error = settingsFragment.errors.get(x);
                    erroredPackages.add(error.getPackageName());
                }

                settingsFragment.dialog.dismiss();
                Dialog dialog2 = new Dialog(settingsFragment.context);
                dialog2.setContentView(R.layout.validator_dialog_inner);
                RecyclerView recyclerView = dialog2.findViewById(R.id.recycler_view);
                ArrayList<ValidatorInfo> validatorInfos = new ArrayList<>();
                for (int i = 0; i < result.size(); i++) {
                    boolean validated = !erroredPackages.contains(result.get(i));
                    ValidatorInfo validatorInfo = new ValidatorInfo(
                            settingsFragment.context,
                            result.get(i),
                            validated,
                            result.get(i).endsWith(".common"));

                    for (int x = 0; x < settingsFragment.errors.size(); x++) {
                        if (result.get(i).equals(settingsFragment.errors.get(x).getPackageName())) {
                            validatorInfo.setPackageError(settingsFragment.errors.get(x));
                            break;
                        }
                    }
                    validatorInfos.add(validatorInfo);
                }
                ValidatorAdapter validatorAdapter = new ValidatorAdapter(validatorInfos);
                recyclerView.setAdapter(validatorAdapter);
                RecyclerView.LayoutManager layoutManager =
                        new LinearLayoutManager(settingsFragment.context);
                recyclerView.setLayoutManager(layoutManager);

                Button button = dialog2.findViewById(R.id.button_done);
                button.setOnClickListener(v -> dialog2.dismiss());

                Window window = dialog2.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                    layoutParams.copyFrom(window.getAttributes());
                    layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                    layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                    window.setAttributes(layoutParams);
                }
                dialog2.show();
            }
        }

        @Override
        protected ArrayList<String> doInBackground(String... sUrl) {
            // First, we have to download the repository list into the cache
            SettingsFragment settingsFragment = ref.get();
            ArrayList<String> packages = new ArrayList<>();
            if (settingsFragment != null) {
                FileDownloader.init(
                        settingsFragment.context,
                        settingsFragment.getString(Systems.checkOreo() ?
                                R.string.validator_o_url : R.string.validator_n_url),
                        "repository_names.xml", VALIDATOR_CACHE);

                FileDownloader.init(
                        settingsFragment.context,
                        settingsFragment.getString(Systems.checkOreo() ?
                                R.string.validator_o_whitelist_url :
                                R.string.validator_n_whitelist_url),
                        "resource_whitelist.xml", VALIDATOR_CACHE);

                List<Repository> repositories =
                        ReadRepositoriesFile.read(
                                settingsFragment.context.getCacheDir().getAbsolutePath() +
                                        VALIDATOR_CACHE_DIR + "repository_names.xml");

                List<ValidatorFilter> whitelist =
                        ReadFilterFile.read(
                                settingsFragment.context.getCacheDir().getAbsolutePath() +
                                        VALIDATOR_CACHE_DIR + "resource_whitelist.xml");

                settingsFragment.errors = new ArrayList<>();
                for (int i = 0; i < repositories.size(); i++) {
                    Repository repository = repositories.get(i);
                    // Now we have to check all the packages
                    String packageName = repository.getPackageName();
                    ValidatorError validatorError = new ValidatorError(packageName);
                    Boolean has_errored = false;

                    String tempPackageName = (packageName.endsWith(".common") ?
                            packageName.substring(0, packageName.length() - 7) :
                            packageName);

                    if (Packages.isPackageInstalled(settingsFragment.context, tempPackageName)) {
                        packages.add(packageName);

                        // Check if there's a bools commit check
                        if (repository.getBools() != null) {
                            FileDownloader.init(settingsFragment.context, repository.getBools(),
                                    tempPackageName + ".bools.xml", VALIDATOR_CACHE);
                            List<String> bools =
                                    ReadResourcesFile.read(
                                            settingsFragment.context.
                                                    getCacheDir().getAbsolutePath() +
                                                    VALIDATOR_CACHE_DIR + tempPackageName +
                                                    ".bools.xml",
                                            "bool");
                            for (int j = 0; j < bools.size(); j++) {
                                boolean validated = Packages.validateResource(
                                        settingsFragment.context,
                                        tempPackageName,
                                        bools.get(j),
                                        "bool");
                                if (validated) {
                                    if (VALIDATE_WITH_LOGS)
                                        Log.d("BoolCheck", "Resource exists: " + bools.get(j));
                                } else {
                                    boolean bypassed = false;
                                    for (int x = 0; x < whitelist.size(); x++) {
                                        String currentPackage = whitelist.get(x)
                                                .getPackageName();
                                        List<String> currentWhitelist = whitelist.get(x)
                                                .getFilter();
                                        if (currentPackage.equals(packageName)) {
                                            if (currentWhitelist.contains(bools.get(j))) {
                                                if (VALIDATE_WITH_LOGS)
                                                    Log.d("BoolCheck",
                                                            "Resource bypassed using filter: " +
                                                                    bools.get(j));
                                                bypassed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bypassed) {
                                        if (VALIDATE_WITH_LOGS)
                                            Log.e("BoolCheck",
                                                    "Resource does not exist: " + bools.get(j));
                                        has_errored = true;
                                        validatorError.addBoolError(
                                                '{' + settingsFragment.getString(
                                                        R.string.resource_boolean) + "} " +
                                                        bools.get(j));
                                    }
                                }
                            }
                        }
                        // Then go through the entire list of colors
                        if (repository.getColors() != null) {
                            FileDownloader.init(settingsFragment.context, repository.getColors(),
                                    tempPackageName + ".colors.xml", VALIDATOR_CACHE);
                            List<String> colors = ReadResourcesFile.read(
                                    settingsFragment.context
                                            .getCacheDir().getAbsolutePath() +
                                            VALIDATOR_CACHE_DIR + tempPackageName + ".colors.xml",
                                    "color");
                            for (int j = 0; j < colors.size(); j++) {
                                boolean validated = Packages.validateResource(
                                        settingsFragment.context,
                                        tempPackageName,
                                        colors.get(j),
                                        "color");
                                if (validated) {
                                    if (VALIDATE_WITH_LOGS)
                                        Log.d("ColorCheck", "Resource exists: " + colors.get(j));
                                } else {
                                    boolean bypassed = false;
                                    for (int x = 0; x < whitelist.size(); x++) {
                                        String currentPackage = whitelist.get(x)
                                                .getPackageName();
                                        List<String> currentWhitelist = whitelist.get(x)
                                                .getFilter();
                                        if (currentPackage.equals(packageName)) {
                                            if (currentWhitelist.contains(colors.get(j))) {
                                                if (VALIDATE_WITH_LOGS)
                                                    Log.d("ColorCheck",
                                                            "Resource bypassed using filter: " +
                                                                    colors.get(j));
                                                bypassed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bypassed) {
                                        if (VALIDATE_WITH_LOGS)
                                            Log.e("ColorCheck",
                                                    "Resource does not exist: " + colors.get(j));
                                        has_errored = true;
                                        validatorError.addBoolError(
                                                '{' + settingsFragment.getString(
                                                        R.string.resource_color) + "} " +
                                                        colors.get(j));
                                    }
                                }
                            }
                        }
                        // Next, dimensions may need to be exposed
                        if (repository.getDimens() != null) {
                            FileDownloader.init(settingsFragment.context, repository.getDimens(),
                                    tempPackageName + ".dimens.xml", VALIDATOR_CACHE);
                            List<String> dimens = ReadResourcesFile.read(
                                    settingsFragment.context.getCacheDir().getAbsolutePath() +
                                            VALIDATOR_CACHE_DIR + tempPackageName +
                                            ".dimens.xml", "dimen");
                            for (int j = 0; j < dimens.size(); j++) {
                                boolean validated = Packages.validateResource(
                                        settingsFragment.context,
                                        tempPackageName,
                                        dimens.get(j),
                                        "dimen");
                                if (validated) {
                                    if (VALIDATE_WITH_LOGS)
                                        Log.d("DimenCheck", "Resource exists: " + dimens.get(j));
                                } else {
                                    boolean bypassed = false;
                                    for (int x = 0; x < whitelist.size(); x++) {
                                        String currentPackage = whitelist.get(x)
                                                .getPackageName();
                                        List<String> currentWhitelist = whitelist.get(x)
                                                .getFilter();
                                        if (currentPackage.equals(packageName)) {
                                            if (currentWhitelist.contains(dimens.get(j))) {
                                                if (VALIDATE_WITH_LOGS)
                                                    Log.d("DimenCheck",
                                                            "Resource bypassed using filter: " +
                                                                    dimens.get(j));
                                                bypassed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bypassed) {
                                        if (VALIDATE_WITH_LOGS)
                                            Log.e("DimenCheck",
                                                    "Resource does not exist: " + dimens.get(j));
                                        has_errored = true;
                                        validatorError.addBoolError(
                                                '{' + settingsFragment.getString(
                                                        R.string.resource_dimension) + '}' +
                                                        ' ' +
                                                        dimens.get(j));
                                    }
                                }
                            }
                        }
                        // Finally, check if styles are exposed
                        if (repository.getStyles() != null) {
                            FileDownloader.init(settingsFragment.context, repository.getStyles(),
                                    tempPackageName + ".styles.xml", VALIDATOR_CACHE);
                            List<String> styles = ReadResourcesFile.read(
                                    settingsFragment.context
                                            .getCacheDir().getAbsolutePath() +
                                            VALIDATOR_CACHE_DIR + tempPackageName + ".styles.xml",
                                    "style");
                            for (int j = 0; j < styles.size(); j++) {
                                boolean validated = Packages.validateResource(
                                        settingsFragment.context,
                                        tempPackageName,
                                        styles.get(j),
                                        "style");
                                if (validated) {
                                    if (VALIDATE_WITH_LOGS)
                                        Log.d("StyleCheck", "Resource exists: " + styles.get(j));
                                } else {
                                    boolean bypassed = false;
                                    for (int x = 0; x < whitelist.size(); x++) {
                                        String currentPackage = whitelist.get(x)
                                                .getPackageName();
                                        List<String> currentWhitelist = whitelist.get(x)
                                                .getFilter();
                                        if (currentPackage.equals(packageName)) {
                                            if (currentWhitelist.contains(styles.get(j))) {
                                                if (VALIDATE_WITH_LOGS)
                                                    Log.d("StyleCheck",
                                                            "Resource bypassed using filter: " +
                                                                    styles.get(j));
                                                bypassed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bypassed) {
                                        if (VALIDATE_WITH_LOGS)
                                            Log.e("StyleCheck",
                                                    "Resource does not exist: " + styles.get(j));
                                        has_errored = true;
                                        validatorError.addBoolError(
                                                '{' + settingsFragment.getString(
                                                        R.string.resource_style) + "} " +
                                                        styles.get(j));
                                    }
                                }
                            }
                        }
                    } else if (VALIDATE_WITH_LOGS)
                        Log.d(SUBSTRATUM_VALIDATOR,
                                "This device does not come built-in with '" + packageName + "', " +
                                        "skipping resource verification...");
                    if (has_errored) settingsFragment.errors.add(validatorError);
                }
            }
            return packages;
        }
    }

}
