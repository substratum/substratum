package projekt.substratum;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

public class ProfileErrorInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String dialog_message = getIntent().getStringExtra("dialog_message");
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.restore_dialog_title))
                .setMessage(dialog_message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_ok), (dialogInterface, i) -> {
                    PackageManager manager = this.getPackageManager();
                    Intent intent = manager.getLaunchIntentForPackage("projekt.substratum");
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    startActivity(intent);
                    finish();
                })
                .setOnCancelListener(dialogInterface -> finish())
                .setCancelable(true)
                .create().show();
    }
}
