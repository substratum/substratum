package projekt.substratum;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import projekt.substratum.config.ThemeManager;

public class RescueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast toast = Toast.makeText(
                getApplicationContext(),
                getString(R.string.rescue_toast),
                Toast.LENGTH_LONG);
        toast.show();
        Handler handler = new Handler();
        handler.postDelayed(() ->
                runOnUiThread(() ->
                        ThemeManager.disableAll(getApplicationContext())), 1000);
        this.finish();
    }
}