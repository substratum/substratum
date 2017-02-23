package projekt.substratum.config;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Calendar;

import projekt.substratum.BuildConfig;

public class FirebaseAnalytics {

    // Save data to Firebase
    public static void backupDebuggableStatistics(Context mContext, String tag, String data,
                                                  String reason) {
        try {
            FirebaseDatabase mDatabaseInstance = FirebaseDatabase.getInstance();
            DatabaseReference mDatabase = mDatabaseInstance.getReference(tag);
            String currentTimeAndDate = java.text.DateFormat.getDateTimeInstance().format(
                    Calendar.getInstance().getTime());
            Account[] accounts = AccountManager.get(mContext).getAccountsByType("com.google");
            String main_acc = "null";
            for (Account account : accounts) {
                if (account.name != null) {
                    main_acc = account.name.replace(".", "(dot)");
                }
            }
            String entryId = main_acc;
            String userId = FirebaseInstanceId.getInstance().getToken();
            DeviceCollection user = new DeviceCollection(
                    currentTimeAndDate,
                    userId,
                    data,
                    reason,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.VERSION_NAME);
            mDatabase.child(entryId).child(data).setValue(user);
        } catch (RuntimeException re) {
            // Suppress Warning
        }
    }

    @IgnoreExtraProperties
    @SuppressWarnings("WeakerAccess")
    private static class DeviceCollection {

        public String CurrentTime;
        public String FireBaseID;
        public String ID;
        public String Reason;
        public int VersionCode;
        public String VersionName;

        public DeviceCollection(String CurrentTime, String FireBaseID, String ID, String Reason,
                                int VersionCode, String VersionName) {
            this.CurrentTime = CurrentTime;
            this.FireBaseID = FireBaseID;
            this.ID = ID;
            this.Reason = Reason;
            this.VersionCode = VersionCode;
            this.VersionName = VersionName;
        }
    }
}