package projekt.substratum.tabs;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.adapters.tabs.overlays.OverlaysItem;

class OverlaysInstance {

    private static final OverlaysInstance ourInstance = new OverlaysInstance();
    Boolean has_failed;
    Integer fail_count;
    StringBuilder failed_packages;
    StringBuilder error_logs;
    Boolean missingType3;
    List<String> final_runner;
    List<String> late_install;
    ArrayList<String> final_command;
    List<OverlaysItem> checkedOverlays;
    double current_amount;
    double total_amount;
    int overlaysWaiting;

    /**
     * Obtain the current instance
     *
     * @return Returns the current instance
     */
    static OverlaysInstance getInstance() {
        ourInstance.reset();
        return ourInstance;
    }

    /**
     * Resets the singleton instance and its values
     */
    void reset() {
        has_failed = false;
        fail_count = 0;
        failed_packages = new StringBuilder();
        error_logs = new StringBuilder();
        missingType3 = false;
        final_runner = new ArrayList<>();
        late_install = new ArrayList<>();
        final_command = new ArrayList<>();
        checkedOverlays = new ArrayList<>();
        current_amount = 0;
        overlaysWaiting = 0;
    }
}